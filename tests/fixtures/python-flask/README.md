# Python Flask Sample

Minimal Flask REST API for DevSecOps pipeline testing.

## Run Locally
```bash
pip install -r requirements.txt
python app.py
```

## Endpoints
| Method | Path         | Description     |
|--------|-------------|-----------------|
| GET    | /api/health | Health check    |
| GET    | /api/info   | Service info    |
| GET    | /api/items  | List items      |

## Test
```bash
pytest
```
