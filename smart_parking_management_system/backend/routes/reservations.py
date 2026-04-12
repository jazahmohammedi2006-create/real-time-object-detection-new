from flask import Blueprint, request, jsonify
from db import db_cursor

reservations_bp = Blueprint("reservations", __name__)


@reservations_bp.get("")
def list_reservations():
    with db_cursor(dictionary=True) as (_, cur):
        cur.execute(
            """
            SELECT r.reservation_id, c.full_name AS customer_name, v.plate_number, s.spot_code,
                   r.reserved_from, r.reserved_to, r.status, r.created_at
            FROM reservations r
            JOIN customers c ON c.customer_id = r.customer_id
            JOIN vehicles v ON v.vehicle_id = r.vehicle_id
            JOIN parking_spots s ON s.spot_id = r.spot_id
            ORDER BY r.reservation_id DESC
            """
        )
        return jsonify(cur.fetchall())


@reservations_bp.post("")
def create_reservation():
    data = request.get_json(force=True)
    with db_cursor(dictionary=True) as (_, cur):
        cur.callproc(
            "sp_create_reservation",
            [
                data["customer_id"],
                data["vehicle_id"],
                data["spot_id"],
                data["reserved_from"],
                data["reserved_to"],
            ],
        )
        for result in cur.stored_results():
            return jsonify(result.fetchone()), 201
    return jsonify({"error": "Reservation could not be created"}), 400


@reservations_bp.patch("/<int:reservation_id>/checkin")
def checkin_reservation(reservation_id: int):
    with db_cursor(dictionary=True) as (_, cur):
        cur.callproc("sp_checkin_reservation", [reservation_id])
        for result in cur.stored_results():
            return jsonify(result.fetchone())
    return jsonify({"rows_affected": 0})


@reservations_bp.patch("/<int:reservation_id>/complete")
def complete_reservation(reservation_id: int):
    data = request.get_json(force=True)
    with db_cursor(dictionary=True) as (_, cur):
        cur.callproc(
            "sp_complete_reservation",
            [reservation_id, data["employee_id"], data["payment_method"]],
        )
        for result in cur.stored_results():
            return jsonify(result.fetchone())
    return jsonify({"error": "Could not complete reservation"}), 400


@reservations_bp.patch("/<int:reservation_id>/cancel")
def cancel_reservation(reservation_id: int):
    with db_cursor(dictionary=True) as (_, cur):
        cur.callproc("sp_cancel_reservation", [reservation_id])
        for result in cur.stored_results():
            return jsonify(result.fetchone())
    return jsonify({"rows_affected": 0})
