from flask import Blueprint, request, jsonify
from db import db_cursor

customers_bp = Blueprint("customers", __name__)


@customers_bp.get("")
def list_customers():
    with db_cursor(dictionary=True) as (_, cur):
        cur.execute("SELECT customer_id, full_name, email, phone, created_at FROM customers ORDER BY customer_id DESC")
        return jsonify(cur.fetchall())


@customers_bp.post("")
def create_customer():
    data = request.get_json(force=True)
    with db_cursor() as (_, cur):
        cur.execute(
            """
            INSERT INTO customers (full_name, email, phone)
            VALUES (%s, %s, %s)
            """,
            (data["full_name"], data["email"], data["phone"]),
        )
        return jsonify({"customer_id": cur.lastrowid}), 201


@customers_bp.post("/<int:customer_id>/vehicles")
def add_vehicle(customer_id: int):
    data = request.get_json(force=True)
    with db_cursor() as (_, cur):
        cur.execute(
            """
            INSERT INTO vehicles (customer_id, plate_number, vehicle_type, brand, model, color)
            VALUES (%s, %s, %s, %s, %s, %s)
            """,
            (
                customer_id,
                data["plate_number"],
                data["vehicle_type"],
                data.get("brand"),
                data.get("model"),
                data.get("color"),
            ),
        )
        return jsonify({"vehicle_id": cur.lastrowid}), 201
