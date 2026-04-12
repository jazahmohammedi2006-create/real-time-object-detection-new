import os
from pathlib import Path
import mysql.connector
from dotenv import load_dotenv

load_dotenv()


def get_server_connection():
    return mysql.connector.connect(
        host=os.getenv("MYSQL_HOST", "localhost"),
        port=int(os.getenv("MYSQL_PORT", "3306")),
        user=os.getenv("MYSQL_USER", "root"),
        password=os.getenv("MYSQL_PASSWORD", ""),
    )


def execute_schema(schema_path: Path):
    with get_server_connection() as conn:
        with conn.cursor() as cur:
            sql = schema_path.read_text(encoding="utf-8")
            for _ in cur.execute(sql, multi=True):
                pass
            conn.commit()


def seed_data():
    db_name = os.getenv("MYSQL_DATABASE", "smart_parking")
    with get_server_connection() as conn:
        with conn.cursor() as cur:
            cur.execute(f"USE {db_name}")

            cur.execute(
                """
                INSERT INTO parking_lots (lot_name, location, total_spots)
                VALUES
                ('Central Plaza', 'Downtown', 50),
                ('Airport Hub', 'Airport Road', 80)
                """
            )

            cur.execute(
                """
                INSERT INTO parking_spots (lot_id, spot_code, spot_type, hourly_rate)
                VALUES
                (1, 'A-01', 'SEDAN', 3.50),
                (1, 'A-02', 'SUV', 4.50),
                (1, 'A-03', 'EV', 5.00),
                (2, 'B-01', 'COMPACT', 2.50),
                (2, 'B-02', 'HANDICAP', 2.00)
                """
            )

            cur.execute(
                """
                INSERT INTO customers (full_name, email, phone)
                VALUES
                ('Alex Carter', 'alex@example.com', '555-1001'),
                ('Nina Shah', 'nina@example.com', '555-1002')
                """
            )

            cur.execute(
                """
                INSERT INTO vehicles (customer_id, plate_number, vehicle_type, brand, model, color)
                VALUES
                (1, 'ABC-1234', 'SEDAN', 'Toyota', 'Camry', 'Black'),
                (2, 'XYZ-9000', 'EV', 'Tesla', 'Model 3', 'White')
                """
            )

            cur.execute(
                """
                INSERT INTO employees (full_name, role_name, email, phone, hired_on)
                VALUES
                ('Maria Lopez', 'MANAGER', 'maria@parking.com', '555-2001', '2024-01-10'),
                ('John Reed', 'ATTENDANT', 'john@parking.com', '555-2002', '2024-03-18')
                """
            )

            cur.execute(
                """
                CALL sp_create_reservation(1, 1, 1, '2026-04-12 10:00:00', '2026-04-12 12:00:00')
                """
            )
            cur.execute("CALL sp_checkin_reservation(1)")
            cur.execute("CALL sp_complete_reservation(1, 1, 'CARD')")

            conn.commit()


if __name__ == "__main__":
    schema_file = Path(__file__).parent / "schema.sql"
    execute_schema(schema_file)
    seed_data()
    print("Database initialized successfully.")
