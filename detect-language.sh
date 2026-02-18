#!/bin/bash
# detect-language.sh - Universal DevSecOps Pipeline Language Detector
# Detects project language, build tool, and package type based on file presence.

set -e

# Function to write output to a file if needed, or just stdout
# Usage: write_output "KEY" "VALUE"
write_output() {
    local key="$1"
    local value="$2"
    echo "${key}=${value}"
}

detect_language() {
    local detected=false

    # JAVA Detection
    if [ -f "pom.xml" ]; then
        write_output "LANGUAGE" "java"
        write_output "BUILD_TOOL" "maven"
        write_output "PACKAGE_TYPE" "jar"
        detected=true
    elif [ -f "build.gradle" ] || [ -f "build.gradle.kts" ]; then
        write_output "LANGUAGE" "java"
        write_output "BUILD_TOOL" "gradle"
        write_output "PACKAGE_TYPE" "jar"
        detected=true
    
    # NODE.JS Detection
    elif [ -f "package.json" ]; then
        write_output "LANGUAGE" "nodejs"
        write_output "BUILD_TOOL" "npm"
        write_output "PACKAGE_TYPE" "docker"
        detected=true
        
    # PYTHON Detection
    elif [ -f "requirements.txt" ] || [ -f "Pipfile" ] || [ -f "pyproject.toml" ]; then
        write_output "LANGUAGE" "python"
        write_output "BUILD_TOOL" "pip"
        write_output "PACKAGE_TYPE" "docker"
        detected=true

    # GOLANG Detection
    elif [ -f "go.mod" ]; then
        write_output "LANGUAGE" "golang"
        write_output "BUILD_TOOL" "go"
        write_output "PACKAGE_TYPE" "binary"
        detected=true

    # RUST Detection
    elif [ -f "Cargo.toml" ]; then
        write_output "LANGUAGE" "rust"
        write_output "BUILD_TOOL" "cargo"
        write_output "PACKAGE_TYPE" "binary"
        detected=true
    fi

    if [ "$detected" = true ]; then
        write_output "DETECTED" "true"
        echo "TIMESTAMP=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
        exit 0
    else
        echo "LANGUAGE=unknown"
        echo "ERROR=No recognizable project files found"
        exit 1
    fi
}

# Run detection
detect_language
