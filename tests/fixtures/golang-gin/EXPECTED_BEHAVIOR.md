# Expected Framework Behavior — Go Gin

## Detection
| Field        | Expected Value |
|--------------|---------------|
| LANGUAGE     | golang        |
| BUILD_TOOL   | go            |
| PACKAGE_TYPE | binary        |
| DETECTED     | true          |

## Build
- **Command:** `go build -o app .`
- **Artifact:** `app` binary

## Security Scans
| Category  | Tool               |
|-----------|--------------------|
| SAST      | Gosec + Semgrep    |
| SCA       | Nancy / govulncheck|
| Secrets   | Gitleaks           |
| Container | Trivy              |
