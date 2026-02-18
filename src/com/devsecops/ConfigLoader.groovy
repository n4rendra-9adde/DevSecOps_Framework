// ConfigLoader.groovy - Universal DevSecOps Pipeline Configuration Loader
// Loads .devsecops.yml and provides typed access to configuration with sensible defaults.

/**
 * Default configuration applied when no .devsecops.yml is present or when
 * specific keys are missing from the user-supplied file.
 */
def getDefaultConfig() {
    return [
        version: "1.0",
        language: [
            override: null,
            version: null
        ],
        build: [
            skip_tests: false,
            custom_command: null,
            timeout_minutes: 60
        ],
        security: [
            fail_on: "HIGH",
            skip_scans: [],
            sonarqube: [
                project_key: null,
                quality_gate_wait: false,
                exclusions: "**/target/**,**/build/**,**/node_modules/**"
            ],
            semgrep: [
                config: "auto"
            ],
            trivy: [
                severity: "HIGH,CRITICAL",
                ignore_unfixed: false
            ]
        ],
        deployment: [
            staging: [
                url: null,
                health_check: "/health",
                timeout_seconds: 60
            ],
            production: [
                url: null,
                requires_approval: true,
                approvers: ["admin", "release-manager"]
            ]
        ],
        notifications: [
            slack: [
                channel: "#devops-alerts",
                webhook_url: null,
                on_success: false,
                on_failure: true,
                on_approval: true
            ],
            email: [
                recipients: [],
                on_success: true,
                on_failure: true,
                on_approval: true
            ]
        ],
        performance: [
            max_parallel_scans: 3,
            container_build_timeout: 600
        ]
    ]
}

// ──────────────────────────────────────────────────────────────
// Core Loading
// ──────────────────────────────────────────────────────────────

/**
 * Loads configuration from .devsecops.yml, merging with defaults.
 * @param workspaceDir Absolute path to workspace root
 * @return Map Merged configuration
 */
def loadConfig(String workspaceDir) {
    def configFile = "${workspaceDir}/.devsecops.yml"
    def defaults = getDefaultConfig()

    if (!fileExists(configFile)) {
        echo "ℹ️ No .devsecops.yml found — using framework defaults"
        return defaults
    }

    try {
        def userConfig = readYaml file: configFile
        def merged = deepMerge(defaults, userConfig ?: [:])
        echo "✅ Loaded .devsecops.yml (version: ${merged.version})"
        return merged
    } catch (Exception e) {
        echo "⚠️ Failed to parse .devsecops.yml: ${e.getMessage()}"
        echo "Falling back to framework defaults"
        return defaults
    }
}

// ──────────────────────────────────────────────────────────────
// Accessor Helpers
// ──────────────────────────────────────────────────────────────

/** Returns the security failure threshold string (e.g. "HIGH"). */
def getSecurityThreshold(Map config) {
    return config?.security?.fail_on ?: "HIGH"
}

/** Returns true if the named scan should be skipped per config. */
def shouldSkipScan(Map config, String scanName) {
    return config?.security?.skip_scans?.contains(scanName) ?: false
}

/** Returns the language override, or null if auto-detect should be used. */
def getLanguageOverride(Map config) {
    return config?.language?.override
}

/** Returns a custom build command, or null. */
def getCustomBuildCommand(Map config) {
    return config?.build?.custom_command
}

/** Returns whether tests should be skipped. */
def shouldSkipTests(Map config) {
    return config?.build?.skip_tests ?: false
}

/** Returns the list of production approvers. */
def getApprovers(Map config) {
    return (config?.deployment?.production?.approvers ?: ["admin"]).join(",")
}

/** Returns the Semgrep config string to use. */
def getSemgrepConfig(Map config) {
    return config?.security?.semgrep?.config ?: "auto"
}

/** Returns the Trivy severity filter. */
def getTrivySeverity(Map config) {
    return config?.security?.trivy?.severity ?: "HIGH,CRITICAL"
}

// ──────────────────────────────────────────────────────────────
// Utility
// ──────────────────────────────────────────────────────────────

/**
 * Deep-merges two maps. Keys in overlay override keys in base.
 * Nested maps are merged recursively; all other types are replaced.
 */
def deepMerge(Map base, Map overlay) {
    def result = [:] + base  // shallow clone
    overlay.each { key, value ->
        if (value instanceof Map && result[key] instanceof Map) {
            result[key] = deepMerge(result[key] as Map, value as Map)
        } else if (value != null) {
            result[key] = value
        }
    }
    return result
}

return this
