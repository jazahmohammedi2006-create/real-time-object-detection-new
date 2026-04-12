# Smart Parking Management System (DBMS Project)

A complete end-to-end DBMS project using **MySQL + Flask + plain HTML/CSS/JS**.

## Highlights
- Normalized relational schema with strong integrity constraints.
- 7 core tables: parking lots, spots, customers, vehicles, employees, reservations, transactions.
- 4 stored procedures for reservation lifecycle.
- 3 triggers, including double-booking prevention.
- Raw SQL backend (no ORM), organized by modules.
- 5 analytical reports exposed via REST APIs.
- User-friendly 5-page dashboard frontend.

## Project Structure

```
smart_parking_management_system/
├── backend/
│   ├── app.py
│   ├── db.py
│   └── routes/
│       ├── customers.py
│       ├── reservations.py
│       ├── spots.py
│       └── reports.py
├── frontend/
│   ├── index.html
│   ├── spots.html
│   ├── reservation.html
│   ├── history.html
│   ├── reports.html
│   ├── app.js
│   └── styles.css
├── schema.sql
├── init_db.py
├── requirements.txt
└── .env.example
```

## Setup
1. Create MySQL user/database access.
2. Copy environment file:
   ```bash
   cp .env.example .env
   ```
3. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```
4. Initialize schema + sample data:
   ```bash
   python init_db.py
   ```
5. Start app:
   ```bash
   python backend/app.py
   ```
6. Open `http://localhost:5000`.

## API Overview
- `GET /api/customers`
- `POST /api/customers`
- `POST /api/customers/<customer_id>/vehicles`
- `GET /api/spots`
- `GET /api/reservations`
- `POST /api/reservations`
- `PATCH /api/reservations/<id>/checkin`
- `PATCH /api/reservations/<id>/complete`
- `PATCH /api/reservations/<id>/cancel`
- `GET /api/reports/occupancy`
- `GET /api/reports/revenue`
- `GET /api/reports/top-customers`
- `GET /api/reports/spot-utilization`
- `GET /api/reports/reservation-status`

## Accuracy & Data Integrity Notes
- Time-overlap conflicts are blocked with trigger-based validation.
- Vehicle ownership is validated before reservation creation.
- Reservation status updates automatically synchronize spot occupancy.
- Monetary transaction values are generated from spot rate × reserved duration.

