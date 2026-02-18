# Expected Framework Behavior — Python Flask

## Detection
| Field        | Expected Value |
|--------------|---------------|
| LANGUAGE     | python        |
| BUILD_TOOL   | pip           |
| PACKAGE_TYPE | docker        |
| DETECTED     | true          |

## Build
- **Command:** `pip install -r requirements.txt`
- **Artifact:** Docker image

## Security Scans
| Category  | Tool               |
|-----------|--------------------|
| SAST      | Bandit + Semgrep   |
| SCA       | pip-audit / Safety |
| Secrets   | Gitleaks           |
| Container | Trivy              |
