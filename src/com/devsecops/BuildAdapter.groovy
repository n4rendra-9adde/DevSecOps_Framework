// detect-language.sh - Universal DevSecOps Pipeline Build Adapter
// Handles build and test logic for multiple languages.

/**
 * Checks if a command is available in the environment
 */
def isToolAvailable(String toolName) {
    try {
        sh(script: "which ${toolName}", returnStatus: true) == 0
    } catch (Exception e) {
        return false
    }
}

/**
 * Safely executes a shell command and returns a result map
 */
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

/**
 * Builds project using detected language and build tool
 */
def buildProject(String language, String buildTool) {
    echo "🔨 Building project: Language=${language}, Tool=${buildTool}"
    
    switch (language) {
        case 'java':
            return buildJava(buildTool)
        case 'nodejs':
            return buildNodejs()
        case 'python':
            return [success: true, artifactPath: "dist/*", errorMessage: ""] // Placeholder
        case 'golang':
            return [success: true, artifactPath: "main", errorMessage: ""] // Placeholder
        case 'rust':
            return [success: true, artifactPath: "target/release/*", errorMessage: ""] // Placeholder
        default:
             echo "⚠️ Unsupported language for build: ${language}"
             return [success: false, artifactPath: "", errorMessage: "Unsupported language"]
    }
}

def buildJava(String buildTool) {
    if (buildTool == 'maven') {
        def hasWrapper = fileExists 'mvnw'
        def mvnCmd = hasWrapper ? './mvnw' : 'mvn'
        
        if (!hasWrapper && !isToolAvailable('mvn')) {
             return [success: false, artifactPath: "", errorMessage: "Maven not found and no wrapper (mvnw) available"]
        }
        
        // Skip tests during build to save time (tests run in separate stage)
        def result = safeSh("${mvnCmd} clean package -DskipTests", "Maven build failed")
        
        if (!result.success) {
            return [success: false, artifactPath: "", errorMessage: result.output]
        }
        
        // Find generated JAR, preferring one without -sources or -javadoc
        // Note: findFiles requires pipeline-utility-steps plugin
        def artifacts = findFiles(glob: 'target/*.jar')
        def mainArtifact = artifacts.find { !it.name.contains('-sources') && !it.name.contains('-javadoc') }
        
        return [
            success: true,
            artifactPath: mainArtifact?.path ?: "target/*.jar",
            errorMessage: ""
        ]
        
    } else if (buildTool == 'gradle') {
        def hasWrapper = fileExists 'gradlew'
        def gradleCmd = hasWrapper ? './gradlew' : 'gradle'
        
        if (!hasWrapper && !isToolAvailable('gradle')) {
             return [success: false, artifactPath: "", errorMessage: "Gradle not found and no wrapper (gradlew) available"]
        }
        
        def result = safeSh("${gradleCmd} clean build -x test", "Gradle build failed")
        
        if (!result.success) {
            return [success: false, artifactPath: "", errorMessage: result.output]
        }
        
        def artifacts = findFiles(glob: 'build/libs/*.jar')
        return [
            success: true,
            artifactPath: artifacts[0]?.path ?: "build/libs/*.jar",
            errorMessage: ""
        ]
    }
    
    return [success: false, artifactPath: "", errorMessage: "Unsupported Java build tool: ${buildTool}"]
}

def buildNodejs() {
    if (!fileExists('package.json')) {
        return [success: false, artifactPath: "", errorMessage: "package.json not found"]
    }
    
    // Prefer npm ci if package-lock.json exists, otherwise npm install
    def cmd = fileExists('package-lock.json') ? 'npm ci' : 'npm install'
    def installResult = safeSh(cmd, "npm install failed")
    
    if (!installResult.success) {
        return [success: false, artifactPath: "", errorMessage: installResult.output]
    }
    
    // Check if build script exists
    def packageJson = readJSON file: 'package.json'
    if (packageJson.scripts?.build) {
        def buildResult = safeSh('npm run build', 'npm build failed')
        if (!buildResult.success) {
            return [success: false, artifactPath: "", errorMessage: buildResult.output]
        }
    }
    
    def artifactPath = 'dist/*'
    if (!fileExists('dist')) {
        artifactPath = fileExists('build') ? 'build/*' : '.'
    }
    
    return [success: true, artifactPath: artifactPath, errorMessage: ""]
}

/**
 * Executes unit tests
 */
def testProject(String language, String buildTool) {
    echo "🧪 Testing project: Language=${language}"
    
    switch (language) {
        case 'java':
            return testJava(buildTool)
        case 'nodejs':
             return testNodejs()
        default:
             echo "⚠️ Tests not implemented for ${language} yet"
             return [success: true, testCount: 0, failedCount: 0]
    }
}

def testJava(String buildTool) {
    def result = [success: true, output: ""]
    
    if (buildTool == 'maven') {
        def hasWrapper = fileExists 'mvnw'
        def mvnCmd = hasWrapper ? './mvnw' : 'mvn'
        result = safeSh("${mvnCmd} test", "Maven tests failed")
        
        // Always try to archive results even if tests failed
        try {
            junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
        } catch (Exception e) {
            echo "⚠️ Could not archive JUnit results: ${e.getMessage()}"
        }

    } else if (buildTool == 'gradle') {
        def hasWrapper = fileExists 'gradlew'
        def gradleCmd = hasWrapper ? './gradlew' : 'gradle'
        result = safeSh("${gradleCmd} test", "Gradle tests failed")
        
        try {
            junit testResults: '**/build/test-results/test/*.xml', allowEmptyResults: true
        } catch (Exception e) {
             echo "⚠️ Could not archive JUnit results: ${e.getMessage()}"
        }
    }
    
    return [success: result.success, testCount: 0, failedCount: 0]
}

def testNodejs() {
    def packageJson = readJSON file: 'package.json'
    if (!packageJson.scripts?.test) {
        echo "⚠️ No test script in package.json, skipping"
        return [success: true, testCount: 0, failedCount: 0]
    }
    
    def result = safeSh('npm test', 'npm tests failed')
    
    // Note: To get JUnit XML from npm, projects typically need jenkins-mocha or jest-junit
    // We'll warn if no XMLs found but won't fail the pipeline step itself (unless npm test failed)
    
    return [success: result.success, testCount: 0, failedCount: 0]
}

def getArtifactPath(String language, String packageType) {
    switch(language) {
        case 'java':
            return packageType == 'jar' ? 'target/*.jar' : 'build/libs/*.jar'
        case 'nodejs':
            return fileExists('dist') ? 'dist/*' : (fileExists('build') ? 'build/*' : '.')
        case 'python':
            return 'dist/*'
        case 'golang':
            return 'main' 
        case 'rust':
            return 'target/release/*'
        default:
            return '*'
    }
}


return this
