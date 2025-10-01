#!/usr/bin/env bash
set -euo pipefail

# Gradle is in project root
APP_RUN="./gradlew :app:run"
# Gradle runs from app/, so tests must be addressed relative to app/
TEST_DIR="../tests/worked"

shopt -s nullglob

passed=()
failed=()

test_files=(tests/worked/*.txt)

if [ ${#test_files[@]} -eq 0 ]; then
    echo "No test files found in tests/worked"
    exit 1
fi

for test_file in "${test_files[@]}"; do
    filename=$(basename "$test_file")
    rel_path="../tests/worked/$filename"

    echo
    echo "========== Test $filename =========="
    echo "Running with args: $rel_path"

    if $APP_RUN --quiet --args="$rel_path"; then
        passed+=("$filename")
    else
        failed+=("$filename")
    fi
done

echo
echo "========== Test Summary =========="
echo "Passed: ${#passed[@]}"
echo "Failed: ${#failed[@]}"

if ((${#passed[@]} > 0)); then
    echo
    echo "✅ Passed tests:"
    for t in "${passed[@]}"; do
        echo " - $t"
    done
fi

if ((${#failed[@]} > 0)); then
    echo
    echo "❌ Failed tests:"
    for t in "${failed[@]}"; do
        echo " - $t"
    done
fi
