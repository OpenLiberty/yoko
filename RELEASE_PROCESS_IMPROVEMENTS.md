# Yoko Release Process Improvement Recommendations

## Executive Summary

This document identifies inconsistencies in the current Yoko release process and provides actionable recommendations to improve consistency, automation, and reliability.

## Current State Analysis

### Identified Inconsistencies

#### 1. **Multiple Release Workflows Without Clear Guidance**

**Issue**: The documentation describes two different workflows:
- **Quick Start (4 steps)**: Direct release from main branch
- **Branch-Based Workflow**: Using `prepareReleaseBranch` and `finalizeRelease` tasks

**Impact**: Confusion about which workflow to use, potential for mistakes

**Evidence**:
- README_RELEASE.md "Quick Start" section promotes direct main branch releases
- `prepareReleaseBranch` and `finalizeRelease` tasks suggest a branch-based approach
- No clear guidance on when to use each approach

#### 2. **Tag Creation Timing Inconsistency**

**Issue**: Conflicting approaches to when Git tags are created:
- `release` task creates tags during GitHub release creation
- `finalizeRelease` task creates tags before GitHub release
- Documentation suggests manual tag creation

**Impact**: Risk of orphaned tags, failed releases, or duplicate tags

**Evidence**:
```gradle
// In createGitHubRelease task:
def checkTag = "git rev-parse --verify ${releaseVersion}".execute()
if (checkTag.exitValue() == 0) {
    throw new GradleException("Git tag ${releaseVersion} already exists...")
}
```

#### 3. **Interactive Version Bumping Incompatible with CI/CD**

**Issue**: The `bumpVersion` task requires interactive terminal input:
```gradle
def reader = new BufferedReader(new InputStreamReader(System.in))
def choice = reader.readLine()?.trim()
```

**Impact**: Cannot be automated in CI/CD pipelines

#### 4. **No Automated Release Workflow**

**Issue**: No GitHub Actions workflow for automated releases

**Impact**: 
- Manual process prone to human error
- Inconsistent release artifacts
- No automated validation before release

**Evidence**: Only `publish.yml` (for docs) exists, no release workflow

#### 5. **Version Consistency Not Validated**

**Issue**: No automated checks to ensure:
- `gradle.properties` version matches CHANGELOG.md version
- Version follows semantic versioning
- Version hasn't already been released

**Impact**: Potential for version mismatches and failed releases

#### 6. **CHANGELOG Update Process Unclear**

**Issue**: Multiple approaches to updating CHANGELOG:
- Manual editing before release
- `updateChangelog` task using git-cliff
- `prepareReleaseBranch` task auto-updates CHANGELOG

**Impact**: Inconsistent CHANGELOG format and content

#### 7. **Rollback Process Not Tested**

**Issue**: Rollback instructions provided but no validation that they work

**Impact**: Risk of incomplete rollbacks leaving repository in inconsistent state

## Recommended Improvements

### Priority 1: Critical Consistency Issues

#### 1.1 Standardize on Single Release Workflow

**Recommendation**: Adopt a **branch-based release workflow** as the standard approach.

**Rationale**:
- Allows for release preparation without blocking main branch
- Enables review and validation before release
- Supports hotfix releases from release branches
- Industry best practice for production releases

**Implementation**:

```markdown
## Standard Release Workflow

1. **Create Release Branch**
   ```bash
   ./gradlew prepareReleaseBranch
   ```
   This creates `release/X.Y.Z` branch and updates CHANGELOG

2. **Review and Test**
   - Review CHANGELOG.md changes
   - Run full test suite
   - Perform manual testing if needed

3. **Finalize Release**
   ```bash
   ./gradlew finalizeRelease
   ```
   This creates tag, merges to main, and cleans up

4. **Create GitHub Release**
   ```bash
   ./gradlew createGitHubRelease
   ```
   Or use automated workflow (see below)
```

#### 1.2 Implement Automated Release Workflow

**Recommendation**: Create GitHub Actions workflow for automated releases.

**File**: `.github/workflows/release.yml`

```yaml
name: Release

on:
  push:
    tags:
      - 'v*.*.*'

permissions:
  contents: write

jobs:
  release:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - name: Set up JDK
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Setup Gradle
        uses: gradle/gradle-build-action@v3

      - name: Validate version consistency
        run: |
          TAG_VERSION=${GITHUB_REF#refs/tags/v}
          GRADLE_VERSION=$(grep '^version=' gradle.properties | cut -d'=' -f2)
          
          if [ "$TAG_VERSION" != "$GRADLE_VERSION" ]; then
            echo "Error: Tag version ($TAG_VERSION) doesn't match gradle.properties version ($GRADLE_VERSION)"
            exit 1
          fi
          
          # Check CHANGELOG contains this version
          if ! grep -q "## \[v\?$TAG_VERSION\]" CHANGELOG.md; then
            echo "Error: Version $TAG_VERSION not found in CHANGELOG.md"
            exit 1
          fi

      - name: Build and test
        run: ./gradlew build test

      - name: Assemble release artifacts
        run: ./gradlew assembleRelease

      - name: Generate release notes
        run: ./gradlew generateReleaseNotes

      - name: Create distribution
        run: ./gradlew createDistribution

      - name: Create GitHub Release
        env:
          GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        run: |
          TAG_VERSION=${GITHUB_REF#refs/tags/}
          
          # Get release notes
          RELEASE_NOTES=$(cat build/release/RELEASE_NOTES.md)
          
          # Create release
          gh release create "$TAG_VERSION" \
            --title "Yoko $TAG_VERSION" \
            --notes "$RELEASE_NOTES" \
            build/release/*.jar \
            build/release/*.sha256 \
            build/release/*.sha512 \
            build/distributions/yoko-*-dist.zip

      - name: Notify on failure
        if: failure()
        run: |
          echo "Release failed! Check the workflow logs."
```

#### 1.3 Add Non-Interactive Version Bump

**Recommendation**: Add non-interactive version bump task for CI/CD compatibility.

**Implementation** (add to `build-release.gradle`):

```gradle
task bumpVersionNonInteractive {
    group = 'release'
    description = 'Bump version non-interactively (for CI/CD). Usage: -PversionBump=major|minor|patch'
    
    doLast {
        def bumpType = project.findProperty('versionBump')
        if (!bumpType || !(bumpType in ['major', 'minor', 'patch'])) {
            throw new GradleException("Must specify -PversionBump=major|minor|patch")
        }
        
        def propsFile = file('gradle.properties')
        def props = new Properties()
        propsFile.withInputStream { props.load(it) }
        def currentVersion = props.getProperty('version')
        
        def matcher = currentVersion =~ /^(\d+)\.(\d+)\.(\d+)$/
        if (!matcher.matches()) {
            throw new GradleException("Invalid version format: ${currentVersion}")
        }
        
        def major = matcher.group(1).toInteger()
        def minor = matcher.group(2).toInteger()
        def patch = matcher.group(3).toInteger()
        
        def newVersion = switch(bumpType) {
            case 'major' -> "${major + 1}.0.0"
            case 'minor' -> "${major}.${minor + 1}.0"
            case 'patch' -> "${major}.${minor}.${patch + 1}"
        }
        
        def content = propsFile.text
        propsFile.text = content.replaceAll(/(?m)^version=.*$/, "version=${newVersion}")
        
        println "Version bumped from ${currentVersion} to ${newVersion}"
    }
}
```

#### 1.4 Add Pre-Release Validation Task

**Recommendation**: Create comprehensive validation task to catch issues before release.

**Implementation** (add to `build-release.gradle`):

```gradle
task validateRelease {
    group = 'release'
    description = 'Validates all release prerequisites and consistency checks'
    
    doLast {
        def errors = []
        def warnings = []
        
        // 1. Check version format
        def versionPattern = /^\d+\.\d+\.\d+$/
        if (!(version ==~ versionPattern)) {
            errors.add("Version '${version}' doesn't match semantic versioning (X.Y.Z)")
        }
        
        // 2. Check CHANGELOG contains version
        def changelogFile = file('CHANGELOG.md')
        if (!changelogFile.exists()) {
            errors.add("CHANGELOG.md not found")
        } else {
            def changelog = changelogFile.text
            if (!changelog.contains("## [v${version}]") && !changelog.contains("## [${version}]")) {
                errors.add("Version ${version} not found in CHANGELOG.md")
            }
        }
        
        // 3. Check git tag doesn't exist
        try {
            def checkTag = "git rev-parse --verify v${version}".execute()
            checkTag.waitFor()
            if (checkTag.exitValue() == 0) {
                errors.add("Git tag v${version} already exists")
            }
        } catch (Exception e) {
            // Tag doesn't exist - good
        }
        
        // 4. Check GitHub release doesn't exist
        try {
            def checkRelease = "gh release view v${version}".execute()
            def output = new ByteArrayOutputStream()
            checkRelease.consumeProcessOutput(output, output)
            checkRelease.waitFor()
            if (checkRelease.exitValue() == 0) {
                errors.add("GitHub release v${version} already exists")
            }
        } catch (Exception e) {
            // Release doesn't exist - good
        }
        
        // 5. Check clean git state
        def gitStatus = 'git status --porcelain'.execute().text.trim()
        if (gitStatus) {
            errors.add("Git working directory not clean:\n${gitStatus}")
        }
        
        // 6. Check on correct branch
        def currentBranch = 'git rev-parse --abbrev-ref HEAD'.execute().text.trim()
        if (!currentBranch.startsWith('release/') && currentBranch != 'main') {
            warnings.add("Not on release branch or main (current: ${currentBranch})")
        }
        
        // 7. Check all tests pass
        def testResults = fileTree("${buildDir}/test-results").matching { include '**/*.xml' }
        if (testResults.isEmpty()) {
            warnings.add("No test results found. Run './gradlew test' first")
        }
        
        // Print results
        if (warnings) {
            println "\n⚠️  Warnings:"
            warnings.each { println "  - ${it}" }
        }
        
        if (errors) {
            println "\n❌ Validation Failed:"
            errors.each { println "  - ${it}" }
            throw new GradleException("Release validation failed")
        }
        
        println "\n✅ Release validation passed"
    }
}

// Make release tasks depend on validation
tasks.named('prepareReleaseBranch').configure {
    dependsOn 'validateRelease'
}

tasks.named('createGitHubRelease').configure {
    dependsOn 'validateRelease'
}
```

### Priority 2: Documentation Improvements

#### 2.1 Update README_RELEASE.md

**Recommendation**: Rewrite README_RELEASE.md to reflect standardized workflow.

**Key Changes**:
1. Remove "Quick Start" section that promotes direct main branch releases
2. Emphasize branch-based workflow as the standard
3. Add troubleshooting section
4. Add validation checklist
5. Document automated workflow

#### 2.2 Add Release Checklist

**Recommendation**: Create a release checklist template.

**File**: `.github/RELEASE_CHECKLIST.md`

```markdown
# Release Checklist for Yoko vX.Y.Z

## Pre-Release
- [ ] All tests passing on main branch
- [ ] No critical bugs or security issues
- [ ] Dependencies up to date
- [ ] Documentation updated

## Version Bump
- [ ] Run `./gradlew bumpVersion` (or `bumpVersionNonInteractive`)
- [ ] Commit version change: `git commit -m "chore: bump version to X.Y.Z"`
- [ ] Push to main: `git push origin main`

## Release Branch
- [ ] Run `./gradlew prepareReleaseBranch`
- [ ] Review CHANGELOG.md changes
- [ ] Verify version in gradle.properties matches CHANGELOG
- [ ] Run full test suite: `./gradlew test`
- [ ] Perform manual smoke testing

## Finalize
- [ ] Run `./gradlew validateRelease`
- [ ] Run `./gradlew finalizeRelease`
- [ ] Verify tag created: `git tag -l v*`
- [ ] Verify merged to main: `git log main --oneline -5`

## GitHub Release
- [ ] Automated workflow triggered by tag push
- [ ] Or manually: `./gradlew createGitHubRelease`
- [ ] Verify release at https://github.com/OpenLiberty/yoko/releases
- [ ] Verify all artifacts present (JARs, checksums, distribution)
- [ ] Test download and checksum verification

## Post-Release
- [ ] Announce release (if applicable)
- [ ] Update documentation site
- [ ] Close milestone (if applicable)
- [ ] Create next milestone

## Rollback (if needed)
- [ ] Delete GitHub release: `gh release delete vX.Y.Z --yes`
- [ ] Delete tag: `git tag -d vX.Y.Z && git push origin :refs/tags/vX.Y.Z`
- [ ] Revert version in gradle.properties
- [ ] Revert CHANGELOG.md
```

### Priority 3: Enhanced Automation

#### 3.1 Add Pre-Commit Hooks

**Recommendation**: Add pre-commit hooks to validate version consistency.

**File**: `.pre-commit-config.yaml` (update existing)

```yaml
# Add to existing hooks:
  - repo: local
    hooks:
      - id: version-consistency
        name: Check version consistency
        entry: scripts/check-version-consistency.sh
        language: script
        pass_filenames: false
        files: ^(gradle\.properties|CHANGELOG\.md)$
```

**File**: `scripts/check-version-consistency.sh`

```bash
#!/bin/bash
# Check version consistency between gradle.properties and CHANGELOG.md

set -e

GRADLE_VERSION=$(grep '^version=' gradle.properties | cut -d'=' -f2)

# Check if version exists in CHANGELOG
if ! grep -q "## \[v\?$GRADLE_VERSION\]" CHANGELOG.md; then
    echo "Warning: Version $GRADLE_VERSION from gradle.properties not found in CHANGELOG.md"
    echo "This is OK for development, but required for releases"
fi

exit 0
```

#### 3.2 Add Release Dry-Run Task

**Recommendation**: Add task to simulate release without making changes.

**Implementation** (add to `build-release.gradle`):

```gradle
task dryRunRelease {
    group = 'release'
    description = 'Simulates release process without making any changes'
    
    doLast {
        println """
╔════════════════════════════════════════════════════════════════╗
║                    Release Dry Run - v${version}                    ║
╚════════════════════════════════════════════════════════════════╝

This would perform the following actions:

1. ✓ Validate prerequisites
2. ✓ Build all release artifacts (${releaseProjects.size()} modules)
3. ✓ Generate checksums (SHA-256, SHA-512)
4. ✓ Extract release notes from CHANGELOG.md
5. ✓ Create distribution archive
6. ✓ Create GitHub release: v${version}
7. ✓ Upload ${releaseProjects.size() * 3} JAR files + checksums
8. ✓ Upload distribution ZIP

Artifacts that would be created:
"""
        releaseProjects.each { projectPath ->
            def proj = project(projectPath)
            println "  - ${proj.name}-${version}.jar"
            println "  - ${proj.name}-${version}-sources.jar"
            println "  - ${proj.name}-${version}-javadoc.jar"
        }
        println "  - yoko-${version}-dist.zip"
        
        println """
To proceed with actual release:
  ./gradlew release

To validate without building:
  ./gradlew validateRelease
"""
    }
}
```

## Implementation Plan

### Phase 1: Critical Fixes (Week 1)
1. Add `validateRelease` task
2. Add `bumpVersionNonInteractive` task
3. Update `verifyReleasePrerequisites` to include version consistency checks
4. Test all changes with dry-run

### Phase 2: Automation (Week 2)
1. Create GitHub Actions release workflow
2. Test automated workflow with pre-release
3. Add pre-commit hooks
4. Create release checklist template

### Phase 3: Documentation (Week 3)
1. Rewrite README_RELEASE.md
2. Add troubleshooting guide
3. Document rollback procedures
4. Create video walkthrough (optional)

### Phase 4: Validation (Week 4)
1. Perform test release using new process
2. Gather feedback from team
3. Refine based on feedback
4. Document lessons learned

## Success Metrics

- **Consistency**: 100% of releases follow standardized workflow
- **Automation**: 90% of release steps automated
- **Reliability**: Zero failed releases due to process issues
- **Time**: Release time reduced by 50%
- **Documentation**: All team members can perform releases independently

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Breaking existing workflows | High | Maintain backward compatibility, provide migration guide |
| GitHub Actions failures | Medium | Add comprehensive error handling and notifications |
| Version conflicts | Medium | Automated validation catches issues early |
| Team adoption | Low | Clear documentation and training |

## Conclusion

The current release process has several inconsistencies that can lead to errors and confusion. By implementing these recommendations, we can achieve:

1. **Single Source of Truth**: One standardized workflow
2. **Automation**: Reduced manual steps and human error
3. **Validation**: Automated checks prevent common mistakes
4. **Documentation**: Clear, comprehensive guidance
5. **Reliability**: Consistent, repeatable releases

The improvements are prioritized to address critical issues first while building toward a fully automated, reliable release process.
