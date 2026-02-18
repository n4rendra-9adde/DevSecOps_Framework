#!/bin/bash
# test_detection.sh

pass=0
fail=0

run_test() {
    local dir=$1
    local expected_lang=$2
    
    echo "Testing $dir..."
    cd "tests/fixtures/$dir"
    output=$(../../../detect-language.sh)
    
    if echo "$output" | grep -q "LANGUAGE=$expected_lang"; then
        echo "✅ PASS: Detected $expected_lang"
        ((pass++))
    else
        echo "❌ FAIL: Expected $expected_lang, got:"
        echo "$output"
        ((fail++))
    fi
    cd - > /dev/null
}

run_test "java-maven" "java"
run_test "java-gradle" "java"
run_test "nodejs" "nodejs"
run_test "python" "python"
run_test "golang" "golang"
run_test "rust" "rust"

echo "--------------------------------"
echo "Tests Completed: $((pass+fail))"
echo "Passed: $pass"
echo "Failed: $fail"
