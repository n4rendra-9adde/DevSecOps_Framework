# Expected Framework Behavior — Java Maven

## Detection
| Field        | Expected Value |
|--------------|---------------|
| LANGUAGE     | java          |
| BUILD_TOOL   | maven         |
| PACKAGE_TYPE | jar           |
| DETECTED     | true          |

## Build
- **Command:** `mvn clean package -DskipTests -B`
- **Artifact:** `target/*.jar`

## Security Scans
| Category  | Tool                   |
|-----------|------------------------|
| SAST      | SonarQube + Semgrep    |
| SCA       | OWASP Dependency-Check |
| Secrets   | Gitleaks               |
| Container | Trivy                  |

## Configuration
- Default `.devsecops.yml` (no overrides)
- Threshold: HIGH
