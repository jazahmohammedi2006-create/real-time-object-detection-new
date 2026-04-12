from flask import Blueprint, request, jsonify
from db import db_cursor

spots_bp = Blueprint("spots", __name__)


@spots_bp.get("")
def list_spots():
    lot_id = request.args.get("lot_id")
    spot_type = request.args.get("spot_type")
    occupied = request.args.get("occupied")

    query = """
        SELECT s.spot_id, s.lot_id, l.lot_name, s.spot_code, s.spot_type,
               s.hourly_rate, s.is_active, s.is_occupied
        FROM parking_spots s
        JOIN parking_lots l ON l.lot_id = s.lot_id
        WHERE 1=1
    """
    params = []

    if lot_id:
        query += " AND s.lot_id = %s"
        params.append(lot_id)
    if spot_type:
        query += " AND s.spot_type = %s"
        params.append(spot_type)
    if occupied in {"true", "false"}:
        query += " AND s.is_occupied = %s"
        params.append(occupied == "true")

    query += " ORDER BY s.spot_id"

    with db_cursor(dictionary=True) as (_, cur):
        cur.execute(query, tuple(params))
        return jsonify(cur.fetchall())
