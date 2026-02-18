# Go Gin Sample

Minimal Gin REST API for DevSecOps pipeline testing.

## Run Locally
```bash
go run main.go
```

## Endpoints
| Method | Path         | Description     |
|--------|-------------|-----------------|
| GET    | /api/health | Health check    |
| GET    | /api/info   | Service info    |
| GET    | /api/items  | List items      |

## Test
```bash
go test ./...
```
