#!/bin/bash
# test-build-adapter.sh
# Mocks Jenkins environment and tests BuildAdapter.groovy

# Mock Jenkins 'sh' step and other globals
cat > mock_jenkins.groovy <<EOF
// Mock Jenkins global steps
def sh(Map args) {
    println "[Mock] Executing: \${args.script}"
    def proc = ["/bin/bash", "-c", args.script].execute()
    proc.waitFor()
    if (args.returnStatus) {
        return proc.exitValue()
    }
    return proc.text
}

def sh(String cmd) {
    println "[Mock] Executing: \${cmd}"
    def proc = ["/bin/bash", "-c", cmd].execute()
    proc.waitFor()
    if (proc.exitValue() != 0) {
        throw new Exception("Command failed with exit code \${proc.exitValue()}")
    }
    return proc.text
}

def echo(String msg) {
    println "[Jenkins] \${msg}"
}

def error(String msg) {
    throw new Exception("[Error] \${msg}")
}

def fileExists(String path) {
    return new File(path).exists()
}

def findFiles(Map args) {
    // Basic glob implementation for testing
    def dir = new File(".")
    def files = []
    // This is very simplified, just handles basic * patterns or exact matches
    // In real Jenkins validation we would trust the plugin
    // For this test we just return dummy file objects if the file exists on disk
    
    // Quick hack for specific expected files in tests
    if (args.glob == 'target/*.jar') {
         if (new File("target/my-app-1.0.jar").exists()) return [new File("target/my-app-1.0.jar")]
         return []
    }
    if (args.glob == 'build/libs/*.jar') {
        if (new File("build/libs/my-app.jar").exists()) return [new File("build/libs/my-app.jar")]
        return []
    }
    
    return []
}

def readJSON(Map args) {
    // Simplified JSON reader
    def jsonSlurper = new groovy.json.JsonSlurper()
    return jsonSlurper.parse(new File(args.file))
}

def isUnix() {
    return true
}

def junit(Map args) {
    println "[Mock] JUnit: \${args}"
}

// Load the adapter
def shell = new GroovyShell(this.class.classLoader, new Binding())
def script = shell.parse(new File('src/com/devsecops/BuildAdapter.groovy'))

// Set the delegate to this script so it can call methods defined here (sh, echo, etc)
script.metaClass.methodMissing = { String name, args ->
    // Forward valid Jenkins steps to the mock environment
    if (['sh', 'echo', 'error', 'fileExists', 'findFiles', 'readJSON', 'isUnix', 'junit'].contains(name)) {
        return this.invokeMethod(name, args)
    }
    throw new MissingMethodException(name, this.class, args)
}

// Test Execution
println "Initialising tests..."

// Helper to assert
def assertSuccess(Map result) {
    if (!result.success) {
        throw new Exception("Expected success but got failure: \${result.errorMessage}")
    }
    println "✅ Success"
}

// 1. JAVA MAVEN
println "\\n--- Testing Java Maven ---"
new File("target").mkdirs()
new File("target/my-app-1.0.jar").createNewFile()
def res = script.buildProject('java', 'maven')
assertSuccess(res)
if (res.artifactPath != 'target/my-app-1.0.jar') throw new Exception("Wrong artifact path: \${res.artifactPath}")

// 2. JAVA GRADLE
println "\\n--- Testing Java Gradle ---"
new File("build/libs").mkdirs()
new File("build/libs/my-app.jar").createNewFile()
def res2 = script.buildProject('java', 'gradle')
assertSuccess(res2)

// 3. NODEJS
println "\\n--- Testing Node.js ---"
new File("package.json").write('{"scripts": {"build": "echo building..."}}')
new File("dist").mkdirs()
def res3 = script.buildProject('nodejs', '')
assertSuccess(res3)

println "\\n--------------------------------"
println "All tests passed!"
EOF

# Run using groovy
groovy mock_jenkins.groovy

# Cleanup
rm mock_jenkins.groovy
rm -rf target build dist package.json
