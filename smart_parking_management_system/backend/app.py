from flask import Flask, send_from_directory
from routes.customers import customers_bp
from routes.reservations import reservations_bp
from routes.spots import spots_bp
from routes.reports import reports_bp


def create_app():
    app = Flask(__name__, static_folder="../frontend", static_url_path="")

    app.register_blueprint(customers_bp, url_prefix="/api/customers")
    app.register_blueprint(reservations_bp, url_prefix="/api/reservations")
    app.register_blueprint(spots_bp, url_prefix="/api/spots")
    app.register_blueprint(reports_bp, url_prefix="/api/reports")

    @app.get("/")
    def home():
        return send_from_directory(app.static_folder, "index.html")

    return app


if __name__ == "__main__":
    create_app().run(host="0.0.0.0", port=5000, debug=True)
