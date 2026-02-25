# Yoko Release System

This document provides an overview of the Yoko release system that has been implemented.

## 🎯 Overview

Yoko has a gradle-based release process. The system automatically:

- ✅ Builds all release artifacts (JARs with sources and javadoc)
- ✅ Generates release notes from CHANGELOG.md
- ✅ Creates checksums (SHA-256 and SHA-512) for all artifacts
- ✅ Publishes releases to GitHub with all binaries attached
- ✅ Can be triggered manually via Gradle

## 📁 Files

### Gradle Configuration
- **`build-release.gradle`** - Main release configuration with all Gradle tasks
- **`build.gradle`** - Updated to include release configuration
- **`gradle.properties`** - Version management

## 🚀 Quick Start

### Create a Release Locally (4 Simple Steps)

1. **Initialise the environment**
   ```bash
   sdk env
   ```

3. **Bump the version**
   ```bash
   # Interactive version bump
   gradle bumpVersion
   
   # Then commit the change
   git add gradle.properties
   git commit -m "chore: bump version to X.Y.Z"
   git push origin main
   ```

2. **Update CHANGELOG.md using git-cliff**
   ```bash
   # Update for all commits since last tag
   gradle updateChangelog
   ```

3. **Commit and Push**
   ```bash
   git add -p CHANGELOG.md
   git commit -m "chore: prepare release vX.Y.Z"
   git push origin main
   ```

4. **Create Release** (choose one method):
   ```bash
   gradle release
   ```
   This creates the release locally using the GitHub CLI and creates the tag.


### Build Versions

During development, the build automatically appends build metadata:
- Format: `{version}.{YYYYMMDD}_{gitHash}`
- Example: `1.5.3.20260109_e6f1be5788`

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

## 🎓 Best Practices

1. **Always update CHANGELOG.md** before releasing
2. **Test thoroughly** before creating a release
3. **Use semantic versioning** (e.g. v1.5.3)
4. **Create releases from main branch** only
5. **Verify the release** on GitHub after creation
6. **Keep release notes clear and concise**

## 🔄 Rolling Back Releases

### Manual Rollback

```bash
# Delete GitHub release
gh release delete vX.Y.Z --yes

# Delete tag
git tag -d vX.Y.Z
git push origin :refs/tags/vX.Y.Z

# Delete branch
git push origin --delete release/X.Y.Z

# If merged to main, revert the merge
git checkout main
git revert -m 1 <merge-commit-hash>
git push origin main
```