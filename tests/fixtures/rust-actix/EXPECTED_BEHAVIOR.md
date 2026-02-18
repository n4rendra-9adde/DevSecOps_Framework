# Expected Framework Behavior — Rust Actix

## Detection
| Field        | Expected Value |
|--------------|---------------|
| LANGUAGE     | rust          |
| BUILD_TOOL   | cargo         |
| PACKAGE_TYPE | binary        |
| DETECTED     | true          |

## Build
- **Command:** `cargo build --release`
- **Artifact:** `target/release/devsecops-rust-sample`

## Security Scans
| Category  | Tool               |
|-----------|--------------------|
| SAST      | Cargo-audit + Semgrep |
| SCA       | Cargo-audit        |
| Secrets   | Gitleaks           |
| Container | Trivy              |
