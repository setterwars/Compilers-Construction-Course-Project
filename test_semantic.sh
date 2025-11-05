#!/bin/bash
cd /home/setterwars/Documents/InnopolisUniversity/compilers/CompilersCourse

echo "Testing Semantic Analyzer..."
echo ""

echo "=== Test 1: Undeclared Variable (should report error) ==="
./gradlew run --args="../tests/errors/UndeclaredVariable.txt" --quiet --console=plain 2>&1

echo ""
echo "=== Test 2: Type Mismatch (should report error) ==="
./gradlew run --args="../tests/errors/TypeMismatch.txt" --quiet --console=plain 2>&1

echo ""
echo "=== Test 3: Valid Program (should pass) ==="
./gradlew run --args="../tests/worked/APlusB.txt" --quiet --console=plain 2>&1

echo ""
echo "=== Test 4: Record Member Error (should report error) ==="
./gradlew run --args="../tests/errors/RecordMemberDoesNotExist.txt" --quiet --console=plain 2>&1
