CREATE DATABASE IF NOT EXISTS smart_parking;
USE smart_parking;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS reservations;
DROP TABLE IF EXISTS vehicles;
DROP TABLE IF EXISTS customers;
DROP TABLE IF EXISTS employees;
DROP TABLE IF EXISTS parking_spots;
DROP TABLE IF EXISTS parking_lots;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE parking_lots (
    lot_id INT AUTO_INCREMENT PRIMARY KEY,
    lot_name VARCHAR(100) NOT NULL UNIQUE,
    location VARCHAR(255) NOT NULL,
    total_spots INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (total_spots > 0)
);

CREATE TABLE parking_spots (
    spot_id INT AUTO_INCREMENT PRIMARY KEY,
    lot_id INT NOT NULL,
    spot_code VARCHAR(20) NOT NULL,
    spot_type ENUM('COMPACT', 'SEDAN', 'SUV', 'EV', 'HANDICAP') NOT NULL,
    hourly_rate DECIMAL(8,2) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_occupied BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_spot_per_lot (lot_id, spot_code),
    CHECK (hourly_rate > 0),
    CONSTRAINT fk_spot_lot FOREIGN KEY (lot_id) REFERENCES parking_lots(lot_id)
        ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE customers (
    customer_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE vehicles (
    vehicle_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NOT NULL,
    plate_number VARCHAR(20) NOT NULL UNIQUE,
    vehicle_type ENUM('COMPACT', 'SEDAN', 'SUV', 'EV', 'HANDICAP') NOT NULL,
    brand VARCHAR(50),
    model VARCHAR(50),
    color VARCHAR(30),
    CONSTRAINT fk_vehicle_customer FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
        ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE employees (
    employee_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    role_name ENUM('ADMIN', 'MANAGER', 'ATTENDANT') NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL UNIQUE,
    hired_on DATE NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE reservations (
    reservation_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NOT NULL,
    vehicle_id INT NOT NULL,
    spot_id INT NOT NULL,
    reserved_from DATETIME NOT NULL,
    reserved_to DATETIME NOT NULL,
    status ENUM('BOOKED', 'CHECKED_IN', 'COMPLETED', 'CANCELLED', 'NO_SHOW') NOT NULL DEFAULT 'BOOKED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (reserved_to > reserved_from),
    CONSTRAINT fk_reservation_customer FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_reservation_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_reservation_spot FOREIGN KEY (spot_id) REFERENCES parking_spots(spot_id)
        ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE transactions (
    transaction_id INT AUTO_INCREMENT PRIMARY KEY,
    reservation_id INT NOT NULL,
    employee_id INT,
    amount DECIMAL(10,2) NOT NULL,
    payment_method ENUM('CASH', 'CARD', 'UPI', 'WALLET') NOT NULL,
    payment_status ENUM('PENDING', 'PAID', 'FAILED', 'REFUNDED') NOT NULL DEFAULT 'PENDING',
    paid_at DATETIME,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (amount >= 0),
    CONSTRAINT fk_tx_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(reservation_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_tx_employee FOREIGN KEY (employee_id) REFERENCES employees(employee_id)
        ON DELETE SET NULL ON UPDATE CASCADE
);

DELIMITER $$

CREATE TRIGGER trg_prevent_double_booking
BEFORE INSERT ON reservations
FOR EACH ROW
BEGIN
    IF EXISTS (
        SELECT 1
        FROM reservations r
        WHERE r.spot_id = NEW.spot_id
          AND r.status IN ('BOOKED', 'CHECKED_IN')
          AND NEW.reserved_from < r.reserved_to
          AND NEW.reserved_to > r.reserved_from
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Double booking detected for this parking spot and time range';
    END IF;
END$$

CREATE TRIGGER trg_validate_vehicle_customer
BEFORE INSERT ON reservations
FOR EACH ROW
BEGIN
    DECLARE owner_customer_id INT;
    SELECT customer_id INTO owner_customer_id
    FROM vehicles
    WHERE vehicle_id = NEW.vehicle_id;

    IF owner_customer_id IS NULL OR owner_customer_id <> NEW.customer_id THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Vehicle does not belong to the selected customer';
    END IF;
END$$

CREATE TRIGGER trg_update_spot_occupancy
AFTER UPDATE ON reservations
FOR EACH ROW
BEGIN
    IF NEW.status = 'CHECKED_IN' THEN
        UPDATE parking_spots SET is_occupied = TRUE WHERE spot_id = NEW.spot_id;
    ELSEIF NEW.status IN ('COMPLETED', 'CANCELLED', 'NO_SHOW') THEN
        UPDATE parking_spots SET is_occupied = FALSE WHERE spot_id = NEW.spot_id;
    END IF;
END$$

CREATE PROCEDURE sp_create_reservation(
    IN p_customer_id INT,
    IN p_vehicle_id INT,
    IN p_spot_id INT,
    IN p_reserved_from DATETIME,
    IN p_reserved_to DATETIME
)
BEGIN
    INSERT INTO reservations (customer_id, vehicle_id, spot_id, reserved_from, reserved_to, status)
    VALUES (p_customer_id, p_vehicle_id, p_spot_id, p_reserved_from, p_reserved_to, 'BOOKED');

    SELECT LAST_INSERT_ID() AS reservation_id;
END$$

CREATE PROCEDURE sp_checkin_reservation(
    IN p_reservation_id INT
)
BEGIN
    UPDATE reservations
    SET status = 'CHECKED_IN'
    WHERE reservation_id = p_reservation_id
      AND status = 'BOOKED';

    SELECT ROW_COUNT() AS rows_affected;
END$$

CREATE PROCEDURE sp_complete_reservation(
    IN p_reservation_id INT,
    IN p_employee_id INT,
    IN p_payment_method ENUM('CASH', 'CARD', 'UPI', 'WALLET')
)
BEGIN
    DECLARE v_hours DECIMAL(10,2);
    DECLARE v_amount DECIMAL(10,2);

    SELECT GREATEST(TIMESTAMPDIFF(MINUTE, reserved_from, reserved_to) / 60.0, 0.5) * s.hourly_rate
      INTO v_amount
    FROM reservations r
    JOIN parking_spots s ON r.spot_id = s.spot_id
    WHERE r.reservation_id = p_reservation_id;

    UPDATE reservations
    SET status = 'COMPLETED'
    WHERE reservation_id = p_reservation_id
      AND status IN ('BOOKED', 'CHECKED_IN');

    INSERT INTO transactions (reservation_id, employee_id, amount, payment_method, payment_status, paid_at)
    VALUES (p_reservation_id, p_employee_id, ROUND(v_amount, 2), p_payment_method, 'PAID', NOW());

    SELECT LAST_INSERT_ID() AS transaction_id, ROUND(v_amount, 2) AS amount;
END$$

CREATE PROCEDURE sp_cancel_reservation(
    IN p_reservation_id INT
)
BEGIN
    UPDATE reservations
    SET status = 'CANCELLED'
    WHERE reservation_id = p_reservation_id
      AND status = 'BOOKED';

    SELECT ROW_COUNT() AS rows_affected;
END$$

DELIMITER ;
