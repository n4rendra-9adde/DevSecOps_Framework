# Expected Framework Behavior — Node.js Express

## Detection
| Field        | Expected Value |
|--------------|---------------|
| LANGUAGE     | nodejs        |
| BUILD_TOOL   | npm           |
| PACKAGE_TYPE | docker        |
| DETECTED     | true          |

## Build
- **Command:** `npm ci && npm run build`
- **Artifact:** `dist/` or Docker image

## Security Scans
| Category  | Tool               |
|-----------|--------------------|
| SAST      | ESLint + Semgrep   |
| SCA       | npm audit          |
| Secrets   | Gitleaks           |
| Container | Trivy              |

## Configuration
- Skips SonarQube (not Java)
- Slack channel: `#nodejs-alerts`
