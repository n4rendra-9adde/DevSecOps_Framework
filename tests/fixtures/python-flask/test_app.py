import pytest
from app import app


@pytest.fixture
def client():
    app.config["TESTING"] = True
    with app.test_client() as client:
        yield client


def test_health(client):
    res = client.get("/api/health")
    assert res.status_code == 200
    assert res.get_json()["status"] == "UP"


def test_info(client):
    res = client.get("/api/info")
    data = res.get_json()
    assert data["language"] == "python"
    assert data["version"] == "1.0.0"


def test_items(client):
    res = client.get("/api/items")
    data = res.get_json()
    assert len(data) == 2
