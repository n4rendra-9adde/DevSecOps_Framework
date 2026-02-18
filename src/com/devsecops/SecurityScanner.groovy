// SecurityScanner.groovy - Universal DevSecOps Pipeline Security Scanner
// Orchestrates SAST, SCA, Secret Detection, and Container Scanning per language.

// ──────────────────────────────────────────────────────────────
// Helper Methods
// ──────────────────────────────────────────────────────────────

def safeSh(String command, String errorMessage) {
    try {
        if (isUnix()) {
            sh command
        } else {
            bat command
        }
        return [success: true, output: ""]
    } catch (Exception e) {
        echo "⚠️ ${errorMessage}: ${e.getMessage()}"
        return [success: false, output: e.getMessage()]
    }
}

def isToolAvailable(String toolName) {
    try {
        return sh(script: "which ${toolName}", returnStatus: true) == 0
    } catch (Exception e) {
        return false
    }
}

/**
 * Determines whether the build should fail based on a severity threshold.
 * Parses a generic JSON report looking for severity fields.
 * @param reportFile Path to JSON report
 * @param threshold  One of CRITICAL, HIGH, MEDIUM, LOW, NEVER
 * @return true if at least one finding meets or exceeds the threshold
 */
def shouldFailBuild(String reportFile, String threshold) {
    def levels = ['CRITICAL': 4, 'HIGH': 3, 'MEDIUM': 2, 'LOW': 1, 'NEVER': 0]
    def thresholdLevel = levels[threshold?.toUpperCase()] ?: 3 // default HIGH

    if (!fileExists(reportFile)) {
        echo "⚠️ Report file not found for threshold check: ${reportFile}"
        return false
    }

    try {
        def report = readJSON file: reportFile, returnPojo: true

        // Trivy format
        if (report instanceof Map && report.Results) {
            def found = report.Results.any { r ->
                r.Vulnerabilities?.any { v ->
                    (levels[v.Severity?.toUpperCase()] ?: 0) >= thresholdLevel
                }
            }
            return found
        }

        // Semgrep format
        if (report instanceof Map && report.results) {
            def found = report.results.any { r ->
                (levels[r.extra?.severity?.toUpperCase()] ?: 0) >= thresholdLevel
            }
            return found
        }

        // Bandit format
        if (report instanceof Map && report.results) {
            def found = report.results.any { r ->
                (levels[r.issue_severity?.toUpperCase()] ?: 0) >= thresholdLevel
            }
            return found
        }

        // Gitleaks – any finding is considered HIGH
        if (report instanceof List && report.size() > 0) {
            return thresholdLevel <= levels['HIGH']
        }

    } catch (Exception e) {
        echo "⚠️ Could not parse report for threshold check: ${e.getMessage()}"
    }
    return false
}

// ──────────────────────────────────────────────────────────────
// SAST (Static Application Security Testing)
// ──────────────────────────────────────────────────────────────

def runSAST(String language, String reportDir) {
    echo "🔍 Running SAST for ${language}"

    switch (language) {
        case 'java':    return runSASTJava(reportDir)
        case 'nodejs':  return runSASTNodejs(reportDir)
        case 'python':  return runSASTPython(reportDir)
        case 'golang':  return runSASTGolang(reportDir)
        case 'rust':    return runSASTRust(reportDir)
        default:
            echo "⚠️ No SAST configuration for ${language}, running Semgrep only"
            return runSemgrepGeneric(reportDir)
    }
}

// ── Java SAST ────────────────────────────────────────────────

def runSASTJava(String reportDir) {
    def findings = 0
    def reportFiles = []

    // 1. SonarQube (submit only, do not block on quality gate)
    try {
        def scannerHome = tool 'SonarScanner'
        withSonarQubeEnv('SonarQube') {
            sh """
                ${scannerHome}/bin/sonar-scanner \\
                    -Dsonar.projectKey=\${env.JOB_NAME} \\
                    -Dsonar.sources=. \\
                    -Dsonar.java.binaries=target/classes,build/classes \\
                    -Dsonar.exclusions=**/target/**,**/build/**,**/.mvn/** \\
                    -Dsonar.qualitygate.wait=false
            """
        }
        echo "✅ SonarQube analysis submitted"
    } catch (Exception e) {
        echo "⚠️ SonarQube skipped: ${e.message}"
    }

    // 2. Semgrep
    def semgrepResult = safeSh(
        "semgrep --config=auto --json --output ${reportDir}/semgrep-java.json . || true",
        "Semgrep Java failed"
    )
    if (fileExists("${reportDir}/semgrep-java.json")) {
        try {
            def report = readJSON file: "${reportDir}/semgrep-java.json", returnPojo: true
            findings = report.results?.size() ?: 0
        } catch (Exception ignored) {}
        reportFiles << "${reportDir}/semgrep-java.json"
    }

    echo "SAST Java: ${findings} findings"
    return [success: true, findingsCount: findings, reportFiles: reportFiles]
}

// ── Node.js SAST ─────────────────────────────────────────────

def runSASTNodejs(String reportDir) {
    def findings = 0
    def reportFiles = []

    // 1. ESLint (if configured in project)
    if (fileExists('.eslintrc.js') || fileExists('.eslintrc.json') || fileExists('.eslintrc.yml')) {
        safeSh(
            "npx eslint . --format json --output-file ${reportDir}/eslint-node.json || true",
            "ESLint failed"
        )
        if (fileExists("${reportDir}/eslint-node.json")) {
            reportFiles << "${reportDir}/eslint-node.json"
        }
    }

    // 2. Semgrep for JS/TS
    safeSh(
        "semgrep --config=auto --config=p/javascript --json --output ${reportDir}/semgrep-node.json . || true",
        "Semgrep Node failed"
    )
    if (fileExists("${reportDir}/semgrep-node.json")) {
        try {
            def report = readJSON file: "${reportDir}/semgrep-node.json", returnPojo: true
            findings = report.results?.size() ?: 0
        } catch (Exception ignored) {}
        reportFiles << "${reportDir}/semgrep-node.json"
    }

    echo "SAST Node.js: ${findings} findings"
    return [success: true, findingsCount: findings, reportFiles: reportFiles]
}

// ── Python SAST ──────────────────────────────────────────────

def runSASTPython(String reportDir) {
    def findings = 0
    def reportFiles = []

    // 1. Bandit
    if (isToolAvailable('bandit')) {
        safeSh(
            "bandit -r . -f json -o ${reportDir}/bandit.json --exit-zero || true",
            "Bandit failed"
        )
    } else {
        safeSh(
            "pip install bandit -q && bandit -r . -f json -o ${reportDir}/bandit.json --exit-zero || true",
            "Bandit install/run failed"
        )
    }
    if (fileExists("${reportDir}/bandit.json")) {
        try {
            def report = readJSON file: "${reportDir}/bandit.json", returnPojo: true
            findings = report.results?.size() ?: 0
        } catch (Exception ignored) {}
        reportFiles << "${reportDir}/bandit.json"
    }

    // 2. Semgrep for Python
    safeSh(
        "semgrep --config=auto --config=p/python --json --output ${reportDir}/semgrep-python.json . || true",
        "Semgrep Python failed"
    )
    if (fileExists("${reportDir}/semgrep-python.json")) {
        reportFiles << "${reportDir}/semgrep-python.json"
    }

    echo "SAST Python: ${findings} findings"
    return [success: true, findingsCount: findings, reportFiles: reportFiles]
}

// ── Go SAST ──────────────────────────────────────────────────

def runSASTGolang(String reportDir) {
    def findings = 0
    def reportFiles = []

    // 1. Gosec
    if (isToolAvailable('gosec')) {
        safeSh(
            "gosec -fmt json -out ${reportDir}/gosec.json ./... || true",
            "Gosec failed"
        )
    } else {
        safeSh(
            "go install github.com/securego/gosec/v2/cmd/gosec@latest && " +
            "gosec -fmt json -out ${reportDir}/gosec.json ./... || true",
            "Gosec install/run failed"
        )
    }
    if (fileExists("${reportDir}/gosec.json")) {
        try {
            def report = readJSON file: "${reportDir}/gosec.json", returnPojo: true
            findings = report.Issues?.size() ?: 0
        } catch (Exception ignored) {}
        reportFiles << "${reportDir}/gosec.json"
    }

    // 2. Semgrep for Go
    safeSh(
        "semgrep --config=auto --config=p/golang --json --output ${reportDir}/semgrep-go.json . || true",
        "Semgrep Go failed"
    )
    if (fileExists("${reportDir}/semgrep-go.json")) {
        reportFiles << "${reportDir}/semgrep-go.json"
    }

    echo "SAST Go: ${findings} findings"
    return [success: true, findingsCount: findings, reportFiles: reportFiles]
}

// ── Rust SAST ────────────────────────────────────────────────

def runSASTRust(String reportDir) {
    def findings = 0
    def reportFiles = []

    // 1. cargo-audit (doubles as SAST + SCA for Rust)
    if (isToolAvailable('cargo-audit')) {
        safeSh(
            "cargo audit --json > ${reportDir}/cargo-audit.json || true",
            "Cargo audit failed"
        )
    } else {
        safeSh(
            "cargo install cargo-audit && cargo audit --json > ${reportDir}/cargo-audit.json || true",
            "Cargo audit install/run failed"
        )
    }
    if (fileExists("${reportDir}/cargo-audit.json")) {
        reportFiles << "${reportDir}/cargo-audit.json"
    }

    // 2. Semgrep
    safeSh(
        "semgrep --config=auto --json --output ${reportDir}/semgrep-rust.json . || true",
        "Semgrep Rust failed"
    )
    if (fileExists("${reportDir}/semgrep-rust.json")) {
        reportFiles << "${reportDir}/semgrep-rust.json"
    }

    echo "SAST Rust: ${findings} findings"
    return [success: true, findingsCount: findings, reportFiles: reportFiles]
}

// ── Generic Semgrep fallback ─────────────────────────────────

def runSemgrepGeneric(String reportDir) {
    def findings = 0
    safeSh(
        "semgrep --config=auto --json --output ${reportDir}/semgrep-generic.json . || true",
        "Semgrep failed"
    )
    if (fileExists("${reportDir}/semgrep-generic.json")) {
        try {
            def report = readJSON file: "${reportDir}/semgrep-generic.json", returnPojo: true
            findings = report.results?.size() ?: 0
        } catch (Exception ignored) {}
    }
    return [success: true, findingsCount: findings, reportFiles: ["${reportDir}/semgrep-generic.json"]]
}

// ──────────────────────────────────────────────────────────────
// SCA (Software Composition Analysis)
// ──────────────────────────────────────────────────────────────

def runSCA(String language, String reportDir) {
    echo "📦 Running SCA for ${language}"

    switch (language) {
        case 'java':    return runSCAJava(reportDir)
        case 'nodejs':  return runSCANodejs(reportDir)
        case 'python':  return runSCAPython(reportDir)
        case 'golang':  return runSCAGolang(reportDir)
        case 'rust':    return runSCARust(reportDir)
        default:
            echo "⚠️ No SCA configuration for ${language}"
            return [success: true, vulnerabilitiesFound: 0, reportFiles: []]
    }
}

// ── Java SCA ─────────────────────────────────────────────────

def runSCAJava(String reportDir) {
    def vulnCount = 0
    def reportFiles = []

    def depCheckDir = "${reportDir}/dependency-check"
    sh "mkdir -p ${depCheckDir}"

    def result = safeSh(
        "dependency-check.sh " +
        "--project '\${env.JOB_NAME ?: \"project\"}' " +
        "--scan . " +
        "--format JSON --format HTML " +
        "--out ${depCheckDir} " +
        "--enableExperimental || true",
        "OWASP Dependency-Check failed"
    )

    if (fileExists("${depCheckDir}/dependency-check-report.json")) {
        try {
            def report = readJSON file: "${depCheckDir}/dependency-check-report.json", returnPojo: true
            vulnCount = report.dependencies?.collect { it.vulnerabilities?.size() ?: 0 }?.sum() ?: 0
        } catch (Exception ignored) {}
        reportFiles << "${depCheckDir}/dependency-check-report.json"
        reportFiles << "${depCheckDir}/dependency-check-report.html"
    }

    echo "SCA Java: ${vulnCount} vulnerabilities"
    return [success: true, vulnerabilitiesFound: vulnCount, reportFiles: reportFiles]
}

// ── Node.js SCA ──────────────────────────────────────────────

def runSCANodejs(String reportDir) {
    def vulnCount = 0
    def reportFiles = []

    // npm audit (always available with npm)
    safeSh(
        "npm audit --json > ${reportDir}/npm-audit.json || true",
        "npm audit failed"
    )
    if (fileExists("${reportDir}/npm-audit.json")) {
        try {
            def report = readJSON file: "${reportDir}/npm-audit.json", returnPojo: true
            vulnCount = report.metadata?.vulnerabilities?.total ?: 0
        } catch (Exception ignored) {}
        reportFiles << "${reportDir}/npm-audit.json"
    }

    // Snyk (optional – only if available)
    if (isToolAvailable('snyk')) {
        safeSh(
            "snyk test --json > ${reportDir}/snyk-node.json || true",
            "Snyk failed"
        )
        if (fileExists("${reportDir}/snyk-node.json")) {
            reportFiles << "${reportDir}/snyk-node.json"
        }
    }

    echo "SCA Node.js: ${vulnCount} vulnerabilities"
    return [success: true, vulnerabilitiesFound: vulnCount, reportFiles: reportFiles]
}

// ── Python SCA ───────────────────────────────────────────────

def runSCAPython(String reportDir) {
    def vulnCount = 0
    def reportFiles = []

    // pip-audit (preferred)
    if (isToolAvailable('pip-audit')) {
        safeSh(
            "pip-audit --format=json --output=${reportDir}/pip-audit.json || true",
            "pip-audit failed"
        )
    } else {
        safeSh(
            "pip install pip-audit -q && pip-audit --format=json --output=${reportDir}/pip-audit.json || true",
            "pip-audit install/run failed"
        )
    }
    if (fileExists("${reportDir}/pip-audit.json")) {
        reportFiles << "${reportDir}/pip-audit.json"
    }

    // safety (fallback)
    if (!fileExists("${reportDir}/pip-audit.json")) {
        safeSh(
            "pip install safety -q && safety check --json > ${reportDir}/safety.json || true",
            "Safety failed"
        )
        if (fileExists("${reportDir}/safety.json")) {
            reportFiles << "${reportDir}/safety.json"
        }
    }

    echo "SCA Python: ${vulnCount} vulnerabilities"
    return [success: true, vulnerabilitiesFound: vulnCount, reportFiles: reportFiles]
}

// ── Go SCA ───────────────────────────────────────────────────

def runSCAGolang(String reportDir) {
    def vulnCount = 0
    def reportFiles = []

    // govulncheck (official Go vulnerability checker)
    if (isToolAvailable('govulncheck')) {
        safeSh(
            "govulncheck -json ./... > ${reportDir}/govulncheck.json || true",
            "govulncheck failed"
        )
    } else {
        safeSh(
            "go install golang.org/x/vuln/cmd/govulncheck@latest && " +
            "govulncheck -json ./... > ${reportDir}/govulncheck.json || true",
            "govulncheck install/run failed"
        )
    }
    if (fileExists("${reportDir}/govulncheck.json")) {
        reportFiles << "${reportDir}/govulncheck.json"
    }

    // nancy (for go.sum analysis)
    if (isToolAvailable('nancy') && fileExists('go.sum')) {
        safeSh(
            "nancy sleuth < go.sum > ${reportDir}/nancy.txt || true",
            "Nancy failed"
        )
    }

    echo "SCA Go: ${vulnCount} vulnerabilities"
    return [success: true, vulnerabilitiesFound: vulnCount, reportFiles: reportFiles]
}

// ── Rust SCA ─────────────────────────────────────────────────

def runSCARust(String reportDir) {
    // cargo-audit was already run during SAST, reuse the report
    def reportFiles = []
    if (fileExists("${reportDir}/cargo-audit.json")) {
        reportFiles << "${reportDir}/cargo-audit.json"
    } else {
        safeSh(
            "cargo audit --json > ${reportDir}/cargo-audit.json || true",
            "Cargo audit failed"
        )
        if (fileExists("${reportDir}/cargo-audit.json")) {
            reportFiles << "${reportDir}/cargo-audit.json"
        }
    }

    echo "SCA Rust: report generated"
    return [success: true, vulnerabilitiesFound: 0, reportFiles: reportFiles]
}

// ──────────────────────────────────────────────────────────────
// Universal Scans
// ──────────────────────────────────────────────────────────────

def runSecretScan(String reportDir) {
    echo "🔒 Running secret detection (Gitleaks)"

    safeSh(
        "gitleaks detect --source . --report-format json --report-path ${reportDir}/gitleaks.json || true",
        "Gitleaks failed"
    )

    def secretsFound = 0
    if (fileExists("${reportDir}/gitleaks.json")) {
        try {
            def report = readJSON file: "${reportDir}/gitleaks.json", returnPojo: true
            secretsFound = (report instanceof List) ? report.size() : 0
        } catch (Exception ignored) {}
    }

    echo "Secrets found: ${secretsFound}"
    return [success: true, secretsFound: secretsFound, reportFiles: ["${reportDir}/gitleaks.json"]]
}

def runContainerScan(String imageName, String reportDir) {
    echo "🐳 Running container scan (Trivy) on ${imageName}"

    // JSON for programmatic parsing
    safeSh(
        "trivy image --format json --output ${reportDir}/trivy-container.json --severity HIGH,CRITICAL ${imageName} || true",
        "Trivy JSON scan failed"
    )

    // Table for human review
    safeSh(
        "trivy image --format table --output ${reportDir}/trivy-container.txt --severity HIGH,CRITICAL ${imageName} || true",
        "Trivy table scan failed"
    )

    def vulnCount = 0
    if (fileExists("${reportDir}/trivy-container.json")) {
        try {
            def report = readJSON file: "${reportDir}/trivy-container.json", returnPojo: true
            vulnCount = report.Results?.collect { it.Vulnerabilities?.size() ?: 0 }?.sum() ?: 0
        } catch (Exception ignored) {}
    }

    echo "Container vulnerabilities: ${vulnCount}"
    return [
        success: true,
        vulnerabilities: [total: vulnCount],
        reportFiles: ["${reportDir}/trivy-container.json", "${reportDir}/trivy-container.txt"]
    ]
}

return this
