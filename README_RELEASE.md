# Yoko Release System

This document provides an overview of the Yoko release system that has been implemented.

## 🎯 Overview

The Yoko project now has a comprehensive, automated release process similar to GoReleaser, but fully integrated with Gradle. The system automatically:

- ✅ Builds all release artifacts (JARs with sources and javadoc)
- ✅ Generates release notes from CHANGELOG.md
- ✅ Creates checksums (SHA-256 and SHA-512) for all artifacts
- ✅ Publishes releases to GitHub with all binaries attached
- ✅ Can be triggered manually via Gradle or automatically via Git tags

## 📁 Files

### Gradle Configuration
- **`build-release.gradle`** - Main release configuration with all Gradle tasks
- **`build.gradle`** - Updated to include release configuration
- **`gradle.properties`** - Version management

### GitHub Actions
- **`.github/workflows/release.yml`** - Automated release workflow triggered by version tags

### Scripts
- **`scripts/test-release.sh`** - Test script to verify release configuration

## 🚀 Quick Start

### Create a Release (4 Simple Steps)

1. **Bump the version (if needed)**
   ```bash
   # Interactive version bump
   ./gradlew bumpVersion
   
   # Then commit the change
   git add gradle.properties
   git commit -m "chore: bump version to X.Y.Z"
   git push origin main
   ```

2. **Update CHANGELOG.md using git-cliff**
   ```bash
   # Update for all commits since last tag
   ./gradlew updateChangelog
   
   # Or update for specific tag
   ./gradlew updateChangelogForTag -PreleaseTag=v1.5.3
   ```

3. **Commit and Push**
   ```bash
   git add CHANGELOG.md
   git commit -m "chore: prepare release v1.5.3"
   git push origin main
   ```

4. **Create Release** (choose one method):

   **Method A: Automated (Recommended)**
   ```bash
   git tag v1.5.3
   git push origin v1.5.3
   ```
   GitHub Actions will create the release. If CHANGELOG.md doesn't already contain
   this version, it will be automatically generated. If you've already manually
   edited the CHANGELOG for this version, it will be preserved.
   
   **Method B: Manual via Gradle**
   ```bash
   ./gradlew release
   ```
   
   **Method C: Manual via GitHub UI**
   - Go to Actions → Release workflow
   - Click "Run workflow"
   - Enter version: `v1.5.3`

## 📦 Available Gradle Tasks

All release tasks are accessible from Gradle:

```bash
# View all release tasks
./gradlew tasks --group=release

# Bump semantic version interactively
./gradlew bumpVersion

# Prepare release branch (creates branch, updates CHANGELOG)
./gradlew prepareReleaseBranch -PreleaseVersion=1.5.3

# Finalize release (tag, merge to main, cleanup)
./gradlew finalizeRelease -PreleaseVersion=1.5.3

# Update CHANGELOG with git-cliff (manual)
./gradlew updateChangelog
./gradlew updateChangelogForTag -PreleaseTag=v1.5.3

# Verify prerequisites
./gradlew verifyReleasePrerequisites

# Build release artifacts
./gradlew assembleRelease

# Generate release notes
./gradlew generateReleaseNotes

# Create distribution archive
./gradlew createDistribution

# Create GitHub release
./gradlew createGitHubRelease
```

## 📌 Version Management

The project version is managed in `gradle.properties`:

```properties
version=1.5.2
```

### Bumping the Version

Use the interactive `bumpVersion` task:

```bash
./gradlew bumpVersion
```

This will:
1. Read the current version from `gradle.properties`
2. Parse it as semantic version (major.minor.patch)
3. Prompt you to choose which component to bump:
   - **Major**: 1.5.2 → 2.0.0 (breaking changes)
   - **Minor**: 1.5.2 → 1.6.0 (new features)
   - **Patch**: 1.5.2 → 1.5.3 (bug fixes)
4. Update `gradle.properties` with the new version
5. Provide next steps for committing the change

### Build Versions

During development, the build automatically appends build metadata:
- Format: `{version}.{YYYYMMDD}_{gitHash}`
- Example: `1.5.2.20260109_e6f1be5788`

For releases, the version tag (e.g., `v1.5.3`) determines the release version.

## 🔧 How It Works

### Local Release Process (via Gradle)

When you run `./gradlew release`:

1. **Verification** - Checks prerequisites (clean git, CHANGELOG, gh CLI, etc.)
2. **Assembly** - Builds all JARs (main, sources, javadoc) for release modules
3. **Checksums** - Generates SHA-256 and SHA-512 checksums for all artifacts
4. **Release Notes** - Extracts latest version from CHANGELOG.md
5. **Distribution** - Creates a complete distribution ZIP archive
6. **GitHub Release** - Uses GitHub CLI to create release with all artifacts

### Automated Release Process (via GitHub Actions)

When you trigger the workflow with version `1.5.3`:

1. **Create Release Branch** - Creates `release/1.5.3` and pushes to origin
2. **Install git-cliff** - Installs git-cliff for CHANGELOG generation
3. **Update CHANGELOG** - Automatically updates CHANGELOG.md using git-cliff
4. **Commit CHANGELOG** - Commits CHANGELOG changes to release branch
5. **Build & Test** - Runs full build and test suite
6. **Create Artifacts** - Builds JARs, sources, javadoc, checksums
7. **Create Annotated Tag** - Creates `v1.5.3` with annotation
8. **Create GitHub Release** - Publishes release with all artifacts
9. **Merge to Main** - Fast-forward merges release branch to main
10. **Cleanup** - Deletes release branch (local and remote)
2. **Build & Test** - Compiles and tests the entire project
3. **Assembly** - Builds all release artifacts
4. **Release Creation** - Creates GitHub release with all binaries
5. **Notification** - GitHub notifies watchers of the new release

## 📋 Release Artifacts

Each release includes:

### Per Module (6 modules)
- `{module}-{version}.jar` - Main library
- `{module}-{version}-sources.jar` - Source code
- `{module}-{version}-javadoc.jar` - API documentation
- `{module}-{version}.jar.sha256` - SHA-256 checksum
- `{module}-{version}.jar.sha512` - SHA-512 checksum

### Distribution
- `yoko-{version}-dist.zip` - Complete distribution archive containing:
  - All JARs and checksums
  - LICENSE, NOTICE, README.md
  - CHANGELOG.md
  - RELEASE_NOTES.md

### Release Modules
1. `yoko-osgi` - OSGi support
2. `yoko-util` - Utility classes
3. `yoko-spec-corba` - CORBA specification
4. `yoko-rmi-spec` - RMI specification
5. `yoko-rmi-impl` - RMI implementation
6. `yoko-core` - Core ORB implementation

## 🔐 Security Features

- **Checksums**: SHA-256 and SHA-512 for all artifacts
- **GitHub Signatures**: Releases signed by GitHub
- **Verification**: Prerequisites checked before release
- **Clean State**: Requires clean git working directory

## 📚 Documentation

This file (README_RELEASE.md) contains all release documentation.

## 🧪 Testing

Test the release configuration:

```bash
./scripts/test-release.sh
```

This verifies:
- ✅ Build configuration
- ✅ Release scripts
- ✅ GitHub Actions workflow
- ✅ Documentation
- ✅ Gradle tasks
- ✅ Prerequisites (Java, gh CLI)

## 🆚 Comparison with GoReleaser

| Feature | GoReleaser | Yoko Release System |
|---------|------------|---------------------|
| Language | Go | Java/Gradle |
| Configuration | `.goreleaser.yml` | `build-release.gradle` |
| Trigger | Git tags | Git tags or manual |
| Artifacts | Binaries | JARs (main, sources, javadoc) |
| Checksums | ✅ | ✅ |
| Release Notes | ✅ | ✅ (from CHANGELOG) |
| GitHub Integration | ✅ | ✅ |
| Local Execution | ✅ | ✅ (via Gradle) |
| CI/CD Integration | ✅ | ✅ (GitHub Actions) |

## 🎓 Best Practices

1. **Always update CHANGELOG.md** before releasing
2. **Test thoroughly** before creating a release
3. **Use semantic versioning** (v1.5.3)
4. **Create releases from main branch** only
5. **Verify the release** on GitHub after creation
6. **Keep release notes clear and concise**

## 🐛 Troubleshooting

### Common Issues

| Issue | Solution |
|-------|----------|
| `gh not found` | Install GitHub CLI: `brew install gh` |
| `Not authenticated` | Run: `gh auth login` |
| `Dirty working directory` | Commit or stash changes |
| `No version in CHANGELOG` | Add version entry to CHANGELOG.md |
| `Build fails` | Run `./gradlew build` to see errors |

## 📞 Support

For questions or issues:

1. Review this documentation
2. Check existing releases for examples
3. Run `./scripts/test-release.sh` to verify setup
4. Open an issue on GitHub

## 🎉 Summary

The Yoko release system provides:

- **Automation** - Minimal manual steps required
- **Consistency** - Same process every time
- **Transparency** - All steps visible and documented
- **Flexibility** - Multiple ways to trigger releases
- **Integration** - Works seamlessly with GitHub
- **Gradle-native** - All accessible via Gradle commands

Everything is accessible from Gradle, just like you requested! 🚀

## 🧪 Testing Releases

### Dry-Run Test

Test the release process without making any changes:

```bash
./gradlew testRelease -PreleaseVersion=1.5.3
```

This dry-run:
- ✅ Checks git status and prerequisites
- ✅ Verifies branch/tag availability
- ✅ Previews CHANGELOG generation
- ✅ Tests the build
- ✅ **Makes NO changes** to git or remote repositories

### Local Testing

Test locally without pushing to origin:

```bash
# Create test branch locally
git checkout -b release/1.5.3-test

# Test CHANGELOG generation
./gradlew updateChangelogForTag -PreleaseTag=v1.5.3-test

# Test build
./gradlew clean build test

# Clean up
git checkout main
git branch -D release/1.5.3-test
```

## 🔄 Rolling Back Releases

### Automated Rollback

```bash
# Complete rollback (deletes tag, release, branch)
./gradlew rollbackRelease -PreleaseVersion=1.5.3

# Keep branch for investigation
./gradlew rollbackRelease -PreleaseVersion=1.5.3 -PkeepBranch=true
```

### Rollback Scenarios

**Before merge to main:**
- Use automated rollback - cleanest option
- No impact on main branch

**After merge to main (no one pulled):**
- Rollback release
- Force-reset main (⚠️ use with caution)

**After merge to main (others pulled):**
- Use `git revert` to preserve history
- Safe for shared branches

### Manual Rollback

```bash
# Delete GitHub release
gh release delete v1.5.3 --yes

# Delete tag
git tag -d v1.5.3
git push origin :refs/tags/v1.5.3

# Delete branch
git push origin --delete release/1.5.3

# If merged to main, revert the merge
git checkout main
git revert -m 1 <merge-commit-hash>
git push origin main
```