#!/bin/bash
# Copyright 2026 IBM Corporation and others.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# SPDX-License-Identifier: Apache-2.0

set -e

echo "=========================================="
echo "Testing Yoko Release Process"
echo "=========================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test counter
TESTS_PASSED=0
TESTS_FAILED=0

# Function to print test result
test_result() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓${NC} $2"
        ((TESTS_PASSED++))
    else
        echo -e "${RED}✗${NC} $2"
        ((TESTS_FAILED++))
    fi
}

# Test 1: Check if build.gradle exists
echo "Test 1: Checking build configuration..."
if [ -f "build.gradle" ] && grep -q "build-release.gradle" build.gradle; then
    test_result 0 "build.gradle includes release configuration"
else
    test_result 1 "build.gradle missing or doesn't include release configuration"
fi

# Test 2: Check if build-release.gradle exists
echo "Test 2: Checking release script..."
if [ -f "build-release.gradle" ]; then
    test_result 0 "build-release.gradle exists"
else
    test_result 1 "build-release.gradle not found"
fi

# Test 3: Check if GitHub workflow exists
echo "Test 3: Checking GitHub Actions workflow..."
if [ -f ".github/workflows/release.yml" ]; then
    test_result 0 "Release workflow exists"
else
    test_result 1 "Release workflow not found"
fi

# Test 4: Check if CHANGELOG.md exists
echo "Test 4: Checking CHANGELOG.md..."
if [ -f "CHANGELOG.md" ]; then
    test_result 0 "CHANGELOG.md exists"
else
    test_result 1 "CHANGELOG.md not found"
fi

# Test 5: Check if documentation exists
echo "Test 5: Checking documentation..."
if [ -f "docs/RELEASE_PROCESS.md" ] && [ -f "docs/RELEASE_QUICK_START.md" ]; then
    test_result 0 "Release documentation exists"
else
    test_result 1 "Release documentation missing"
fi

# Test 6: Verify Gradle tasks are available
echo "Test 6: Checking Gradle tasks..."
if ./gradlew tasks --all 2>/dev/null | grep -q "assembleRelease"; then
    test_result 0 "Release tasks are available"
else
    test_result 1 "Release tasks not found"
fi

# Test 7: Check Java version
echo "Test 7: Checking Java version..."
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -ge 11 ]; then
    test_result 0 "Java 11+ is installed (version: $JAVA_VERSION)"
else
    test_result 1 "Java 11+ required (found: $JAVA_VERSION)"
fi

# Test 8: Check if gh CLI is available (optional)
echo "Test 8: Checking GitHub CLI (optional)..."
if command -v gh &> /dev/null; then
    test_result 0 "GitHub CLI (gh) is installed"
else
    echo -e "${YELLOW}⚠${NC} GitHub CLI (gh) not found (optional for local releases)"
fi

# Test 9: Try to build release artifacts (dry run)
echo "Test 9: Testing artifact assembly..."
if ./gradlew assembleRelease --dry-run &> /dev/null; then
    test_result 0 "Release assembly task is valid"
else
    test_result 1 "Release assembly task has errors"
fi

# Test 10: Verify release projects are configured
echo "Test 10: Checking release projects..."
EXPECTED_PROJECTS=("yoko-osgi" "yoko-util" "yoko-spec-corba" "yoko-rmi-spec" "yoko-rmi-impl" "yoko-core" "testify")
ALL_FOUND=true
for project in "${EXPECTED_PROJECTS[@]}"; do
    if ! grep -q ":$project" settings.gradle; then
        ALL_FOUND=false
        echo -e "  ${RED}✗${NC} Project $project not found in settings.gradle"
    fi
done
if [ "$ALL_FOUND" = true ]; then
    test_result 0 "All release projects are configured"
else
    test_result 1 "Some release projects are missing"
fi

# Summary
echo ""
echo "=========================================="
echo "Test Summary"
echo "=========================================="
echo -e "Passed: ${GREEN}${TESTS_PASSED}${NC}"
echo -e "Failed: ${RED}${TESTS_FAILED}${NC}"
echo ""

if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ All tests passed!${NC}"
    echo ""
    echo "The release process is properly configured."
    echo "You can now create releases using:"
    echo "  ./gradlew release"
    echo ""
    echo "Or by pushing a version tag:"
    echo "  git tag v1.5.3"
    echo "  git push origin v1.5.3"
    exit 0
else
    echo -e "${RED}✗ Some tests failed.${NC}"
    echo ""
    echo "Please fix the issues above before creating releases."
    echo "See docs/RELEASE_PROCESS.md for more information."
    exit 1
fi

# Made with Bob
