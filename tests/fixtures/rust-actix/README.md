# Rust Actix-web Sample

Minimal Actix-web REST API for DevSecOps pipeline testing.

## Run Locally
```bash
cargo run
```

## Endpoints
| Method | Path         | Description     |
|--------|-------------|-----------------|
| GET    | /api/health | Health check    |
| GET    | /api/info   | Service info    |
| GET    | /api/items  | List items      |

## Test
```bash
cargo test
```
