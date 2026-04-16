# Yoko Release Guide

This document describes the standardized release process for Yoko.

## 🎯 Overview

Yoko uses a **branch-based release workflow** with Gradle automation. The process ensures:

- ✅ Consistent version management
- ✅ Automated validation before release
- ✅ Complete release artifacts (JARs, sources, javadoc, checksums)
- ✅ Automated CHANGELOG generation
- ✅ GitHub releases with all binaries

## 📋 Prerequisites

Before starting a release, ensure you have:

1. **Git** - For version control
2. **GitHub CLI (`gh`)** - For creating releases
   - Install: https://cli.github.com/
   - Authenticate: `gh auth login`
3. **git-cliff** (optional) - For CHANGELOG generation
   - Install: https://git-cliff.org/docs/installation
4. **SDKMAN** (recommended) - For Java version management
   - Install: https://sdkman.io/

## 🚀 Standard Release Workflow

### Step 1: Prepare Your Environment

```bash
# Initialize SDKMAN environment (if using)
sdk env

# Ensure you're on main branch with latest changes
git checkout main
git pull origin main

# Verify everything is clean
git status
```

### Step 2: Bump the Version

Choose the appropriate version bump (major, minor, or patch):

```bash
# Interactive version bump (recommended)
./gradlew bumpVersion

# Or non-interactive (for automation)
./gradlew bumpVersionNonInteractive -PversionBump=patch
```

This updates `gradle.properties` with the new version.

**Commit the version change:**

```bash
git add gradle.properties
git commit -m "chore: bump version to X.Y.Z"
git push origin main
```

### Step 3: Update CHANGELOG

Generate release notes from git commits:

```bash
./gradlew updateChangelog
```

This uses `git-cliff` to extract commits since the last release and updates `CHANGELOG.md`.

**Review and commit the CHANGELOG:**

```bash
# Review the changes
git diff CHANGELOG.md

# Stage only the relevant changes
git add -p CHANGELOG.md

# Commit
git commit -m "chore: update CHANGELOG for vX.Y.Z"
git push origin main
```

### Step 4: Validate Release Readiness

Run comprehensive validation checks:

```bash
./gradlew validateRelease
```

This checks:
- ✓ Version format (semantic versioning)
- ✓ CHANGELOG contains current version
- ✓ Git working directory is clean
- ✓ No existing tag for this version
- ✓ No existing GitHub release
- ✓ GitHub CLI is installed and authenticated
- ✓ Tests have been run

**Fix any issues** before proceeding.

### Step 5: Create the Release

Execute the complete release process:

```bash
./gradlew release
```

This will:
1. Verify all prerequisites
2. Build all release artifacts (6 modules × 3 JARs each)
3. Generate SHA-256 and SHA-512 checksums
4. Extract release notes from CHANGELOG
5. Create distribution archive
6. Create Git tag `vX.Y.Z`
7. Create GitHub release with all artifacts

### Step 6: Verify the Release

1. **Check GitHub Release:**
   - Visit: https://github.com/OpenLiberty/yoko/releases
   - Verify all artifacts are present
   - Verify release notes are correct

2. **Test Artifacts:**
   ```bash
   # Download a JAR and verify checksum
   wget https://github.com/OpenLiberty/yoko/releases/download/vX.Y.Z/yoko-core-X.Y.Z.jar
   wget https://github.com/OpenLiberty/yoko/releases/download/vX.Y.Z/yoko-core-X.Y.Z.jar.sha256
   sha256sum -c yoko-core-X.Y.Z.jar.sha256
   ```

3. **Verify Git Tag:**
   ```bash
   git fetch --tags
   git tag -l vX.Y.Z
   git show vX.Y.Z
   ```

## 📦 Release Artifacts

Each release includes:

### Per Module (6 modules)
- `{module}-{version}.jar` - Main library
- `{module}-{version}-sources.jar` - Source code
- `{module}-{version}-javadoc.jar` - API documentation
- `{module}-{version}.jar.sha256` - SHA-256 checksum
- `{module}-{version}.jar.sha512` - SHA-512 checksum

### Distribution
- `yoko-{version}-dist.zip` - Complete distribution containing:
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

## 🔧 Available Gradle Tasks

| Task | Description |
|------|-------------|
| `bumpVersion` | Interactive version bump (major/minor/patch) |
| `bumpVersionNonInteractive` | Non-interactive version bump (use `-PversionBump=patch`) |
| `updateChangelog` | Update CHANGELOG.md using git-cliff |
| `validateRelease` | Comprehensive validation of release readiness |
| `verifyReleasePrerequisites` | Check prerequisites (tools, auth, etc.) |
| `assembleRelease` | Build all release artifacts |
| `generateReleaseNotes` | Extract release notes from CHANGELOG |
| `createDistribution` | Create distribution ZIP archive |
| `createGitHubRelease` | Create GitHub release with artifacts |
| `release` | Complete release process (recommended) |

## 🔄 Version Management

### Semantic Versioning

Yoko follows [Semantic Versioning](https://semver.org/):

- **Major (X.0.0)**: Breaking changes
- **Minor (0.X.0)**: New features, backward compatible
- **Patch (0.0.X)**: Bug fixes, backward compatible

### Development Versions

During development, builds automatically append metadata:
- Format: `{version}.{YYYYMMDD}_{gitHash}`
- Example: `1.6.1.20260416_a1b2c3d`

### Release Versions

Release versions use the exact version from `gradle.properties`:
- Format: `X.Y.Z`
- Example: `1.6.1`

## 🔐 Security

### Checksums

All artifacts include:
- **SHA-256**: Fast, widely supported
- **SHA-512**: More secure, recommended for verification

### Verification

```bash
# Verify SHA-256
sha256sum -c artifact.jar.sha256

# Verify SHA-512
sha512sum -c artifact.jar.sha512
```

### GitHub Signatures

All releases are signed by GitHub's release system.

## 🐛 Troubleshooting

### "Git working directory is not clean"

**Solution:** Commit or stash your changes:
```bash
git status
git add .
git commit -m "your message"
# or
git stash
```

### "Git tag vX.Y.Z already exists"

**Solution:** Either use a different version or delete the tag:
```bash
# Delete local tag
git tag -d vX.Y.Z

# Delete remote tag
git push origin :refs/tags/vX.Y.Z
```

### "GitHub release vX.Y.Z already exists"

**Solution:** Delete the release first:
```bash
gh release delete vX.Y.Z --yes
```

### "Version X.Y.Z not found in CHANGELOG.md"

**Solution:** Update the CHANGELOG:
```bash
./gradlew updateChangelog
git add CHANGELOG.md
git commit -m "chore: update CHANGELOG for vX.Y.Z"
```

### "GitHub CLI (gh) not found"

**Solution:** Install GitHub CLI:
- macOS: `brew install gh`
- Linux: https://github.com/cli/cli/blob/trunk/docs/install_linux.md
- Windows: https://github.com/cli/cli/releases

### "GitHub authentication not configured"

**Solution:** Authenticate with GitHub:
```bash
gh auth login
```

## 🔄 Rolling Back a Release

If you need to roll back a release:

### 1. Delete GitHub Release

```bash
gh release delete vX.Y.Z --yes
```

### 2. Delete Git Tag

```bash
# Delete local tag
git tag -d vX.Y.Z

# Delete remote tag
git push origin :refs/tags/vX.Y.Z
```

### 3. Revert Version Changes

```bash
# Revert the version bump commit
git revert <commit-hash>

# Or manually edit gradle.properties
# Then commit the change
git add gradle.properties
git commit -m "chore: revert version to previous"
git push origin main
```

### 4. Update CHANGELOG (if needed)

Remove or update the release entry in CHANGELOG.md.

## 📚 Best Practices

1. **Always run tests** before releasing:
   ```bash
   ./gradlew test
   ```

2. **Use semantic versioning** consistently

3. **Keep CHANGELOG up to date** with meaningful entries

4. **Validate before releasing**:
   ```bash
   ./gradlew validateRelease
   ```

5. **Test the release artifacts** after publishing

6. **Announce releases** to relevant channels

7. **Create releases from main branch** only

8. **Never force-push** to main after a release

## 🎓 Quick Reference

### Complete Release in 5 Commands

```bash
# 1. Bump version
./gradlew bumpVersion
git add gradle.properties && git commit -m "chore: bump version to X.Y.Z" && git push

# 2. Update CHANGELOG
./gradlew updateChangelog
git add CHANGELOG.md && git commit -m "chore: update CHANGELOG for vX.Y.Z" && git push

# 3. Validate
./gradlew validateRelease

# 4. Release
./gradlew release

# 5. Verify
open https://github.com/OpenLiberty/yoko/releases
```

## 📞 Support

For questions or issues with the release process:

1. Check this documentation
2. Review the troubleshooting section
3. Check existing GitHub issues
4. Create a new issue with the `release` label

## 📝 Release Checklist

Use this checklist for each release:

- [ ] Environment prepared (`sdk env`)
- [ ] On main branch with latest changes
- [ ] Version bumped in gradle.properties
- [ ] CHANGELOG.md updated
- [ ] All tests passing (`./gradlew test`)
- [ ] Validation passed (`./gradlew validateRelease`)
- [ ] Release created (`./gradlew release`)
- [ ] GitHub release verified
- [ ] Artifacts downloaded and checksums verified
- [ ] Git tag verified
- [ ] Documentation updated (if needed)
- [ ] Release announced (if applicable)
