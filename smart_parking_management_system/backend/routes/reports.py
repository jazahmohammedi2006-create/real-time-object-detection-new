from flask import Blueprint, jsonify
from db import db_cursor

reports_bp = Blueprint("reports", __name__)


@reports_bp.get("/occupancy")
def occupancy_report():
    with db_cursor(dictionary=True) as (_, cur):
        cur.execute(
            """
            SELECT l.lot_id, l.lot_name,
                   COUNT(s.spot_id) AS total_spots,
                   SUM(CASE WHEN s.is_occupied THEN 1 ELSE 0 END) AS occupied_spots,
                   ROUND((SUM(CASE WHEN s.is_occupied THEN 1 ELSE 0 END) / COUNT(s.spot_id)) * 100, 2) AS occupancy_pct
            FROM parking_lots l
            JOIN parking_spots s ON s.lot_id = l.lot_id
            GROUP BY l.lot_id, l.lot_name
            ORDER BY occupancy_pct DESC
            """
        )
        return jsonify(cur.fetchall())


@reports_bp.get("/revenue")
def revenue_report():
    with db_cursor(dictionary=True) as (_, cur):
        cur.execute(
            """
            SELECT DATE(t.paid_at) AS paid_date,
                   COUNT(*) AS transactions,
                   ROUND(SUM(t.amount), 2) AS revenue
            FROM transactions t
            WHERE t.payment_status = 'PAID'
            GROUP BY DATE(t.paid_at)
            ORDER BY paid_date DESC
            """
        )
        return jsonify(cur.fetchall())


@reports_bp.get("/top-customers")
def top_customers_report():
    with db_cursor(dictionary=True) as (_, cur):
        cur.execute(
            """
            SELECT c.customer_id, c.full_name,
                   COUNT(r.reservation_id) AS total_reservations
            FROM customers c
            LEFT JOIN reservations r ON r.customer_id = c.customer_id
            GROUP BY c.customer_id, c.full_name
            ORDER BY total_reservations DESC, c.full_name
            LIMIT 10
            """
        )
        return jsonify(cur.fetchall())


@reports_bp.get("/spot-utilization")
def spot_utilization_report():
    with db_cursor(dictionary=True) as (_, cur):
        cur.execute(
            """
            SELECT s.spot_id, s.spot_code,
                   COUNT(r.reservation_id) AS booking_count,
                   ROUND(COALESCE(SUM(TIMESTAMPDIFF(MINUTE, r.reserved_from, r.reserved_to)), 0) / 60, 2) AS hours_reserved
            FROM parking_spots s
            LEFT JOIN reservations r ON r.spot_id = s.spot_id
            GROUP BY s.spot_id, s.spot_code
            ORDER BY booking_count DESC, hours_reserved DESC
            LIMIT 20
            """
        )
        return jsonify(cur.fetchall())


@reports_bp.get("/reservation-status")
def reservation_status_report():
    with db_cursor(dictionary=True) as (_, cur):
        cur.execute(
            """
            SELECT status, COUNT(*) AS total
            FROM reservations
            GROUP BY status
            ORDER BY total DESC
            """
        )
        return jsonify(cur.fetchall())
