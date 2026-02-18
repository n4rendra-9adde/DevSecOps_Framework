# Universal DevSecOps Framework — Developer Guide

## 1. Quick Start (5 minutes)

### Step 1: Add the Pipeline to Your Project

Copy `Jenkinsfile-universal` to your repository root. That's it — no other files are required.

```bash
cp Jenkinsfile-universal /path/to/your/project/
git add Jenkinsfile-universal
git commit -m "Add Universal DevSecOps pipeline"
git push
```

### Step 2: Create Jenkins Job

1. Open Jenkins → **New Item** → **Pipeline**
2. Set **Pipeline Definition** → "Pipeline script from SCM"
3. Point to your repository, set **Script Path** to `Jenkinsfile-universal`
4. Save and **Build Now**

### Step 3: Verify

Check the build console for:
```
✅ Initialized: Language=java, Tool=maven, Threshold=HIGH
```

If you see `LANGUAGE=unknown`, your repo is missing a recognizable build file (see [Troubleshooting](#troubleshooting)).

---

## 2. How It Works

```
┌─ Initialize ─────────────────────────────────────────────────┐
│  detect-language.sh → ConfigLoader → BuildAdapter            │
│  SecurityScanner → NotificationManager → TrendAnalyzer       │
└──────────────────────────────────────────────────────────────┘
         │
         ▼
┌─ Build → Test ───────────────────────────────────────────────┐
│  Language-specific build & test via BuildAdapter              │
└──────────────────────────────────────────────────────────────┘
         │
         ▼
┌─ Security Scans (parallel) ──────────────────────────────────┐
│  ┌──────────┐  ┌──────────┐  ┌──────────┐                   │
│  │ Secrets  │  │  SAST    │  │   SCA    │                   │
│  │ Gitleaks │  │ Semgrep  │  │ Per-lang │                   │
│  └──────────┘  └──────────┘  └──────────┘                   │
└──────────────────────────────────────────────────────────────┘
         │
         ▼
┌─ Container Scan → Security Gate → Approval → Report ─────────┐
│  Trivy (if Docker) → threshold check → input → HTML report   │
└──────────────────────────────────────────────────────────────┘
```

### Supported Languages

| Language  | Build File        | Build Tool | SAST             | SCA                   |
|-----------|-------------------|------------|------------------|-----------------------|
| Java      | `pom.xml`         | Maven      | SonarQube+Semgrep| OWASP Dependency-Check|
| Java      | `build.gradle`    | Gradle     | SonarQube+Semgrep| OWASP Dependency-Check|
| Node.js   | `package.json`    | npm        | ESLint+Semgrep   | npm audit             |
| Python    | `requirements.txt`| pip        | Bandit+Semgrep   | pip-audit             |
| Go        | `go.mod`          | go         | Gosec+Semgrep    | Nancy / govulncheck   |
| Rust      | `Cargo.toml`      | cargo      | Cargo-audit+Semgrep | Cargo-audit        |

---

## 3. Configuration Reference

### `.devsecops.yml` — Full Schema

Place at your repository root to override defaults. All keys are optional.

```yaml
version: "1.0"

# Override auto-detected language
language:
  override: "java"        # Force specific language
  version: "17"           # Hint for tooling

# Build configuration
build:
  skip_tests: false                    # Skip test stage entirely
  custom_command: "./mvnw package"     # Replace auto-detected build command
  timeout_minutes: 60                  # Per-stage timeout

# Security scan configuration
security:
  fail_on: "HIGH"                      # CRITICAL | HIGH | MEDIUM | LOW | NEVER
  skip_scans:                          # Scans to skip
    - "sonarqube"
  sonarqube:
    project_key: "custom-key"
    quality_gate_wait: false
    exclusions: "**/generated/**"
  semgrep:
    config: "auto"                     # or "p/ci", "p/security-audit"
  trivy:
    severity: "HIGH,CRITICAL"
    ignore_unfixed: false

# Deployment
deployment:
  production:
    requires_approval: true
    approvers: ["admin", "release-manager"]

# Notifications
notifications:
  slack:
    channel: "#security-alerts"
    on_success: false
    on_failure: true
    on_approval: true
  email:
    recipients: ["team@example.com"]
    on_failure: true

# Performance
performance:
  max_parallel_scans: 3
  container_build_timeout: 600
```

### Common Recipes

**Skip SonarQube (non-Java projects):**
```yaml
security:
  skip_scans: ["sonarqube"]
```

**Only fail on critical vulnerabilities:**
```yaml
security:
  fail_on: "CRITICAL"
```

**Custom build command:**
```yaml
build:
  custom_command: "./mvnw clean package -Pproduction -DskipTests"
```

**Skip tests during security-only runs:**
```yaml
build:
  skip_tests: true
```

---

## 4. Extending the Framework

### Adding a New Language

**Time estimate: < 4 hours**

#### Step 1 — `detect-language.sh`
```bash
elif [ -f "mix.exs" ]; then
    echo "LANGUAGE=elixir"
    echo "BUILD_TOOL=mix"
    echo "PACKAGE_TYPE=docker"
```

#### Step 2 — `BuildAdapter.groovy`
Add a case to `buildProject()` and `testProject()`:
```groovy
case 'elixir':
    safeSh("mix deps.get && mix compile")
    artifactPath = "_build/prod/**"
    break
```

#### Step 3 — `SecurityScanner.groovy`
Add SAST and SCA entries:
```groovy
case 'elixir':
    safeSh("semgrep --config auto --lang elixir --json -o ${reportDir}/semgrep-elixir.json .")
    break
```

#### Step 4 — Test fixture
Create `tests/fixtures/elixir-phoenix/` with `mix.exs`, source, tests, and `EXPECTED_BEHAVIOR.md`.

#### Step 5 — Verify
```bash
cd tests/fixtures/elixir-phoenix && bash ../../../detect-language.sh
```

### Adding a New Security Tool

1. Add tool invocation to `SecurityScanner.groovy`
2. Update `shouldFailBuild()` to parse its JSON output format
3. Add metric collection to `TrendAnalyzer.groovy`
4. Update `generateUnifiedReport()` with a new table row

---

## 5. Architecture Overview

### Component Map

| Component              | File                           | Purpose                          |
|------------------------|--------------------------------|----------------------------------|
| Language Detector      | `detect-language.sh`           | File-presence–based detection    |
| Build Adapter          | `BuildAdapter.groovy`          | Language-specific build/test     |
| Security Scanner       | `SecurityScanner.groovy`       | SAST, SCA, Secret, Container     |
| Config Loader          | `ConfigLoader.groovy`          | `.devsecops.yml` parsing         |
| Notification Manager   | `NotificationManager.groovy`   | Slack, Email, alert routing      |
| Trend Analyzer         | `TrendAnalyzer.groovy`         | Build-to-build comparison        |
| Pipeline Orchestrator  | `Jenkinsfile-universal`        | 9-stage pipeline definition      |

### Design Principles

1. **Convention Over Configuration** — works with zero config; `.devsecops.yml` for overrides only
2. **Graceful Degradation** — missing tools logged as warnings, not errors
3. **Parallel Where Possible** — 3 concurrent security scans
4. **Fail Fast on Code, Warn on Security** — build/test errors fail; security findings are configurable
5. **Single Artifact** — one HTML report combines all results
