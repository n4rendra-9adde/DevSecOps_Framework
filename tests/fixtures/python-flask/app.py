from flask import Flask, jsonify

app = Flask(__name__)


@app.route("/api/health")
def health():
    return jsonify({"status": "UP", "service": "python-flask-sample"})


@app.route("/api/info")
def info():
    return jsonify({
        "name": "DevSecOps Python Sample",
        "version": "1.0.0",
        "language": "python",
        "framework": "flask",
    })


@app.route("/api/items")
def items():
    return jsonify([
        {"id": 1, "name": "Item A"},
        {"id": 2, "name": "Item B"},
    ])


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)
