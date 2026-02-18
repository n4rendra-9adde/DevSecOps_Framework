#!/bin/bash
# detect-language.sh - Enhanced with fallback detection

echo "🔍 Scanning for project files..."

# Check at root level first
if [ -f "pom.xml" ]; then 
    echo "LANGUAGE=java"
    echo "BUILD_TOOL=maven"
    echo "PACKAGE_TYPE=jar"
    
elif [ -f "build.gradle" ] || [ -f "build.gradle.kts" ]; then 
    echo "LANGUAGE=java"
    echo "BUILD_TOOL=gradle"
    echo "PACKAGE_TYPE=jar"
    
elif [ -f "package.json" ]; then 
    echo "LANGUAGE=nodejs"
    echo "BUILD_TOOL=npm"
    echo "PACKAGE_TYPE=docker"
    
elif [ -f "requirements.txt" ] || [ -f "setup.py" ] || [ -f "pyproject.toml" ]; then 
    echo "LANGUAGE=python"
    echo "BUILD_TOOL=pip"
    echo "PACKAGE_TYPE=docker"
    
elif [ -f "go.mod" ]; then 
    echo "LANGUAGE=golang"
    echo "BUILD_TOOL=go"
    echo "PACKAGE_TYPE=binary"
    
elif [ -f "Cargo.toml" ]; then 
    echo "LANGUAGE=rust"
    echo "BUILD_TOOL=cargo"
    echo "PACKAGE_TYPE=binary"
    
# Check subdirectories (for monorepos or nested projects)
elif find . -maxdepth 2 -name "pom.xml" | grep -q .; then
    echo "LANGUAGE=java"
    echo "BUILD_TOOL=maven"
    echo "PACKAGE_TYPE=jar"
    echo "NOTE=Found pom.xml in subdirectory"
    
elif find . -maxdepth 2 -name "package.json" | grep -q .; then
    echo "LANGUAGE=nodejs"
    echo "BUILD_TOOL=npm"
    echo "PACKAGE_TYPE=docker"
    echo "NOTE=Found package.json in subdirectory"
    
# Docker-only projects
elif [ -f "Dockerfile" ] || [ -f "docker-compose.yml" ]; then
    echo "LANGUAGE=docker"
    echo "BUILD_TOOL=docker"
    echo "PACKAGE_TYPE=docker"
    echo "NOTE=Docker-only project, no build tool detected"
    
# Static/HTML projects
elif find . -maxdepth 1 -name "*.html" | grep -q .; then
    echo "LANGUAGE=static"
    echo "BUILD_TOOL=none"
    echo "PACKAGE_TYPE=static"
    echo "NOTE=Static HTML project"
    
# Shell scripts
elif find . -maxdepth 1 -name "*.sh" | grep -q .; then
    echo "LANGUAGE=bash"
    echo "BUILD_TOOL=none"
    echo "PACKAGE_TYPE=script"
    echo "NOTE=Bash script project"
    
else 
    echo "LANGUAGE=unknown"
    echo "BUILD_TOOL=unknown"
    echo "PACKAGE_TYPE=unknown"
    echo "DETECTED=false"
    echo "ERROR=No recognizable project files found"
    echo "FILES_FOUND=$(find . -maxdepth 2 -type f -name '*.json' -o -name '*.xml' -o -name '*.yml' -o -name '*.yaml' -o -name 'Dockerfile' -o -name '*.md' | head -10 | tr '\n' ' ')"
    exit 1
fi

echo "DETECTED=true"
echo "TIMESTAMP=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
