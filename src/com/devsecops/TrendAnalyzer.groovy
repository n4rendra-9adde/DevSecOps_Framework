// TrendAnalyzer.groovy - Universal DevSecOps Pipeline Trend Analysis
// Tracks security metrics across builds and computes deltas.

// ──────────────────────────────────────────────────────────────
// Metric Collection
// ──────────────────────────────────────────────────────────────

/**
 * Collects current build metrics from report JSON files.
 * @param reportDir  Path to the reports directory
 * @return Map of aggregated metrics
 */
def collectMetrics(String reportDir) {
    def metrics = [
        timestamp:    new Date().format("yyyy-MM-dd'T'HH:mm:ss'Z'", TimeZone.getTimeZone('UTC')),
        build_number: env.BUILD_NUMBER ?: '0',
        language:     env.LANGUAGE ?: 'unknown',
        sast: [total: 0, critical: 0, high: 0, medium: 0, low: 0],
        sca:  [total: 0, critical: 0, high: 0, medium: 0, low: 0],
        secrets: 0,
        container: [total: 0, critical: 0, high: 0]
    ]

    // ── Semgrep findings ──────────────────────────────────────
    def semgrepFiles = findFiles(glob: "${reportDir}/semgrep-*.json")
    semgrepFiles.each { f ->
        try {
            def r = readJSON file: f.path, returnPojo: true
            r.results?.each { finding ->
                metrics.sast.total++
                def sev = finding.extra?.severity?.toUpperCase() ?: 'LOW'
                if (metrics.sast.containsKey(sev.toLowerCase())) {
                    metrics.sast[sev.toLowerCase()]++
                }
            }
        } catch (Exception ignored) {}
    }

    // ── Gitleaks ──────────────────────────────────────────────
    if (fileExists("${reportDir}/gitleaks.json")) {
        try {
            def r = readJSON file: "${reportDir}/gitleaks.json", returnPojo: true
            metrics.secrets = (r instanceof List) ? r.size() : 0
        } catch (Exception ignored) {}
    }

    // ── Trivy container ───────────────────────────────────────
    if (fileExists("${reportDir}/trivy-container.json")) {
        try {
            def r = readJSON file: "${reportDir}/trivy-container.json", returnPojo: true
            r.Results?.each { res ->
                res.Vulnerabilities?.each { v ->
                    metrics.container.total++
                    def sev = v.Severity?.toUpperCase()
                    if (sev == 'CRITICAL') metrics.container.critical++
                    if (sev == 'HIGH')     metrics.container.high++
                }
            }
        } catch (Exception ignored) {}
    }

    return metrics
}

// ──────────────────────────────────────────────────────────────
// Persistence
// ──────────────────────────────────────────────────────────────

/**
 * Saves current metrics as a build artifact so future builds can read them.
 */
def saveMetrics(Map metrics, String reportDir) {
    writeJSON file: "${reportDir}/metrics.json", json: metrics, pretty: 2
    echo "📊 Metrics saved to ${reportDir}/metrics.json"
}

/**
 * Attempts to load metrics from the previous successful build.
 * Uses Jenkins copyArtifacts plugin if available, otherwise returns null.
 */
def loadPreviousMetrics() {
    try {
        copyArtifacts(
            projectName: env.JOB_NAME,
            filter: 'reports/metrics.json',
            selector: lastSuccessful(),
            target: 'previous-build',
            optional: true
        )
        if (fileExists('previous-build/reports/metrics.json')) {
            return readJSON file: 'previous-build/reports/metrics.json', returnPojo: true
        }
    } catch (Exception e) {
        echo "ℹ️ No previous build metrics available: ${e.getMessage()}"
    }
    return null
}

// ──────────────────────────────────────────────────────────────
// Delta Calculation
// ──────────────────────────────────────────────────────────────

/**
 * Computes the delta between current and previous metrics.
 * @return Map with delta values and direction arrows
 */
def computeTrend(Map current, Map previous) {
    if (!previous) {
        return [
            available: false,
            message: "No previous build to compare"
        ]
    }

    def sastDelta      = current.sast.total      - (previous.sast?.total ?: 0)
    def secretsDelta   = current.secrets          - (previous.secrets ?: 0)
    def containerDelta = current.container.total  - (previous.container?.total ?: 0)

    return [
        available: true,
        previous_build: previous.build_number,
        sast: [
            delta: sastDelta,
            arrow: arrow(sastDelta),
            label: "${Math.abs(sastDelta)} ${sastDelta >= 0 ? 'new' : 'resolved'}"
        ],
        secrets: [
            delta: secretsDelta,
            arrow: arrow(secretsDelta),
            label: "${Math.abs(secretsDelta)} ${secretsDelta >= 0 ? 'new' : 'resolved'}"
        ],
        container: [
            delta: containerDelta,
            arrow: arrow(containerDelta),
            label: "${Math.abs(containerDelta)} ${containerDelta >= 0 ? 'new' : 'resolved'}"
        ]
    ]
}

def arrow(int delta) {
    if (delta > 0) return '↑'
    if (delta < 0) return '↓'
    return '→'
}

// ──────────────────────────────────────────────────────────────
// HTML Snippet for Report
// ──────────────────────────────────────────────────────────────

/**
 * Returns an HTML fragment for embedding in the unified report.
 */
def trendHtml(Map trend) {
    if (!trend.available) {
        return """<div class="card" style="margin-bottom:1.5rem"><h3>Trend Analysis</h3><p style="color:#94a3b8">No previous build data available for comparison.</p></div>"""
    }

    def badgeClass = { int delta -> delta > 0 ? 'badge-red' : (delta < 0 ? 'badge-green' : 'badge-yellow') }

    return """
<div class="card" style="margin-bottom:1.5rem">
<h3>Trend Analysis (vs Build #${trend.previous_build})</h3>
<table>
  <tr><th>Category</th><th>Change</th><th>Direction</th></tr>
  <tr><td>SAST Findings</td><td><span class="badge ${badgeClass(trend.sast.delta)}">${trend.sast.arrow} ${trend.sast.label}</span></td><td>${trend.sast.arrow}</td></tr>
  <tr><td>Secrets</td><td><span class="badge ${badgeClass(trend.secrets.delta)}">${trend.secrets.arrow} ${trend.secrets.label}</span></td><td>${trend.secrets.arrow}</td></tr>
  <tr><td>Container Vulns</td><td><span class="badge ${badgeClass(trend.container.delta)}">${trend.container.arrow} ${trend.container.label}</span></td><td>${trend.container.arrow}</td></tr>
</table>
</div>"""
}

return this
