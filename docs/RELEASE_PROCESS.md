# Yoko Release Process

This document describes the release process for the Yoko project, including how to create releases with binaries published to GitHub.

## Overview

The Yoko project uses a Gradle-based release process that:
- Builds all release artifacts (JARs with sources and javadoc)
- Generates release notes from CHANGELOG.md
- Creates checksums for all artifacts
- Publishes releases to GitHub with all binaries attached
- Can be triggered manually or automatically via Git tags

## Prerequisites

### For Local Releases

1. **Java 11+** - Required for building
2. **Git** - For version control
3. **git-cliff** - For CHANGELOG generation (optional but recommended)
   ```bash
   # Install on macOS
   brew install git-cliff
   
   # Install on Linux
   cargo install git-cliff
   # Or download from: https://git-cliff.org/docs/installation
   
   # Verify installation
   git-cliff --version
   ```

4. **GitHub CLI (`gh`)** - For creating releases
   ```bash
   # Install on macOS
   brew install gh
   
   # Install on Linux
   curl -fsSL https://cli.github.com/packages/githubcli-archive-keyring.gpg | sudo dd of=/usr/share/keyrings/githubcli-archive-keyring.gpg
   echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/githubcli-archive-keyring.gpg] https://cli.github.com/packages stable main" | sudo tee /etc/apt/sources.list.d/github-cli.list > /dev/null
   sudo apt update
   sudo apt install gh
   ```

5. **GitHub Authentication**
   ```bash
   # Authenticate with GitHub
   gh auth login
   
   # Or set environment variable
   export GH_TOKEN=your_github_token
   ```

### For Automated Releases (GitHub Actions)

No additional setup required - the workflow uses `GITHUB_TOKEN` automatically.

## Release Workflow

### 1. Prepare Release Branch

The release process uses a dedicated release branch workflow:

**Option A: Using Gradle (Recommended)**
```bash
# Create release branch and update CHANGELOG automatically
./gradlew prepareReleaseBranch -PreleaseVersion=1.5.3
```

This will:
1. Create a `release/1.5.3` branch
2. Push it to origin
3. Update CHANGELOG.md using git-cliff
4. Commit and push CHANGELOG changes

**Option B: Manual Process**
```bash
# Create release branch
git checkout -b release/1.5.3
git push -u origin release/1.5.3

# Update CHANGELOG
./gradlew updateChangelogForTag -PreleaseTag=v1.5.3

# Commit changes
git add CHANGELOG.md
git commit -m "chore(release): update CHANGELOG for 1.5.3"
git push origin release/1.5.3
```

### 2. Review Release Branch

Review the changes on the release branch:
- Verify CHANGELOG.md is accurate
- Run tests: `./gradlew test`
- Make any necessary adjustments
- Commit and push any additional changes

### 3. Finalize Release (Choose One Method)

#### Method A: Using Gradle (Recommended)

```bash
# From the release/1.5.3 branch
./gradlew finalizeRelease -PreleaseVersion=1.5.3
```

This will:
1. Create an annotated tag `v1.5.3`
2. Push the tag to origin
3. Checkout main branch
4. Fast-forward merge release branch to main
5. Push main to origin
6. Delete the release branch (local and remote)

Then create the GitHub release:
```bash
./gradlew createGitHubRelease
```

#### Method B: Using GitHub Actions (Automated)

1. Go to Actions → Release workflow
2. Click "Run workflow"
3. Enter version: `1.5.3` (without 'v' prefix)
4. Click "Run workflow"

This automatically:
- Creates release branch
- Updates CHANGELOG
- Builds and tests
- Creates annotated tag
- Creates GitHub release
- Merges to main (fast-forward)
- Cleans up release branch

#### Method C: Manual Git Commands

```bash
# From release/1.5.3 branch
git tag -a v1.5.3 -m "Release 1.5.3"
git push origin v1.5.3

# Merge to main
git checkout main
git pull origin main
git merge --ff-only release/1.5.3
git push origin main

# Clean up
git push origin --delete release/1.5.3
git branch -d release/1.5.3
```

Then create GitHub release manually or via Gradle.

### 4. Verify the Release

1. Visit the [Releases page](https://github.com/OpenLiberty/yoko/releases)
2. Verify the release is published with:
   - Correct version number (v1.5.3)
   - Annotated tag
   - Release notes from CHANGELOG
   - All JAR files (main, sources, javadoc)
   - Checksum files (.sha256, .sha512)
   - Distribution archive
3. Verify main branch includes the release changes
4. Verify release branch has been deleted

## Available Gradle Tasks

### Release Tasks

```bash
# Update CHANGELOG using git-cliff
./gradlew updateChangelog

# Update CHANGELOG for specific tag
./gradlew updateChangelogForTag -PreleaseTag=v1.5.3

# Verify all prerequisites for release
./gradlew verifyReleasePrerequisites

# Build all release artifacts (JARs)
./gradlew assembleRelease

# Generate release notes from CHANGELOG.md
./gradlew generateReleaseNotes

# Create distribution archive
./gradlew createDistribution

# Create GitHub release (requires gh CLI)
./gradlew createGitHubRelease

# Complete release process (all of the above)
./gradlew release
```

### Individual Artifact Tasks

```bash
# Build main JARs
./gradlew jar

# Build sources JARs
./gradlew sourcesJar

# Build javadoc JARs
./gradlew javadocJar
```

## Release Artifacts

Each release includes the following artifacts for each module:

### Core Modules
- `yoko-osgi-{version}.jar` - OSGi support
- `yoko-util-{version}.jar` - Utility classes
- `yoko-spec-corba-{version}.jar` - CORBA specification
- `yoko-rmi-spec-{version}.jar` - RMI specification
- `yoko-rmi-impl-{version}.jar` - RMI implementation
- `yoko-core-{version}.jar` - Core ORB implementation

### Additional Files
- `*-sources.jar` - Source code for each module
- `*-javadoc.jar` - API documentation for each module
- `*.sha256` - SHA-256 checksums
- `*.sha512` - SHA-512 checksums
- `yoko-{version}-dist.zip` - Complete distribution archive

## Troubleshooting

### "GitHub CLI (gh) not found"

Install the GitHub CLI:
```bash
# macOS
brew install gh

# Linux (Debian/Ubuntu)
sudo apt install gh

# Windows
winget install GitHub.cli
```

### "GitHub authentication not configured"

Authenticate with GitHub:
```bash
gh auth login
```

Or set the `GH_TOKEN` environment variable:
```bash
export GH_TOKEN=your_personal_access_token
```

### "Git working directory is not clean"

Commit or stash your changes:
```bash
git status
git add .
git commit -m "your message"
# or
git stash
```

### "No version found in CHANGELOG.md"

Ensure CHANGELOG.md has a version entry in the format:
```markdown
## [v1.5.3] - 2026-01-08
```

### Release Fails During Build

Check the build logs:
```bash
./gradlew build --stacktrace
```

Fix any build or test failures before attempting release.

## Version Numbering

Yoko follows semantic versioning with an additional build metadata:

```
major.minor.patch.YYYYMMDD_GITHASH
```

Example: `1.5.2.20260108_a1b2c3d4e5`

- **major.minor.patch** - Semantic version
- **YYYYMMDD** - Build date in UTC
- **GITHASH** - First 10 characters of git commit hash

For releases, use the semantic version part: `v1.5.2`

## Best Practices

1. **Always update CHANGELOG.md** before releasing
2. **Test thoroughly** before creating a release
3. **Use semantic versioning** for version numbers
4. **Create releases from main branch** only
5. **Verify the release** on GitHub after creation
6. **Announce the release** to users and stakeholders
7. **Keep release notes clear and concise**
8. **Include migration notes** for breaking changes

## Rollback Process

If a release needs to be rolled back:

1. Delete the release on GitHub:
   ```bash
   gh release delete v1.5.3
   ```

2. Delete the tag:
   ```bash
   git tag -d v1.5.3
   git push origin :refs/tags/v1.5.3
   ```

3. Fix the issues and create a new release

## CI/CD Integration

The release process integrates with GitHub Actions:

- **Trigger**: Push a tag matching `v*.*.*`
- **Workflow**: `.github/workflows/release.yml`
- **Artifacts**: Automatically uploaded to GitHub release
- **Notifications**: GitHub notifications sent to watchers

## Security Considerations

- All artifacts include SHA-256 and SHA-512 checksums
- Releases are signed with GitHub's release signature
- Use GitHub's security scanning for dependencies
- Review security advisories before releasing

## Support

For questions or issues with the release process:

1. Check this documentation
2. Review existing releases for examples
3. Open an issue on GitHub
4. Contact the maintainers

## References

- [Semantic Versioning](https://semver.org/)
- [GitHub CLI Documentation](https://cli.github.com/manual/)
- [GitHub Releases Documentation](https://docs.github.com/en/repositories/releasing-projects-on-github)
- [Gradle Documentation](https://docs.gradle.org/)

## Testing Releases

### Dry-Run Test

Before creating an actual release, test the process:

```bash
./gradlew testRelease -PreleaseVersion=1.5.3
```

This performs a dry-run that:
- Checks git status and prerequisites
- Verifies branch/tag don't already exist
- Previews CHANGELOG generation
- Tests the build
- **Makes NO changes** to git or remote repositories

### Local Testing

Test the release branch workflow locally without pushing:

```bash
# Create local release branch (don't push)
git checkout -b release/1.5.3-test

# Update CHANGELOG locally
./gradlew updateChangelogForTag -PreleaseTag=v1.5.3-test

# Review changes
git diff

# Test build
./gradlew clean build test

# Clean up test branch
git checkout main
git branch -D release/1.5.3-test
```

## Rolling Back a Release

If you need to undo a release:

### Quick Rollback (Automated)

```bash
./gradlew rollbackRelease -PreleaseVersion=1.5.3
```

This will:
1. Delete the tag `v1.5.3` (local and remote)
2. Delete the GitHub release
3. Delete the release branch (local and remote)

To keep the release branch for investigation:
```bash
./gradlew rollbackRelease -PreleaseVersion=1.5.3 -PkeepBranch=true
```

### Manual Rollback

If the release was already merged to main:

```bash
# 1. Delete the GitHub release
gh release delete v1.5.3 --yes

# 2. Delete the tag
git tag -d v1.5.3
git push origin :refs/tags/v1.5.3

# 3. Delete the release branch
git push origin --delete release/1.5.3
git branch -D release/1.5.3

# 4. Revert the merge on main (if already merged)
git checkout main
git pull origin main

# Find the merge commit
git log --oneline --graph

# Revert the merge commit (use the commit hash)
git revert -m 1 <merge-commit-hash>
git push origin main
```

### Rollback Scenarios

**Scenario 1: Release not yet merged to main**
- Use `./gradlew rollbackRelease -PreleaseVersion=1.5.3`
- This is the cleanest rollback

**Scenario 2: Release merged to main, no one has pulled**
- Rollback the release
- Force-reset main to before the merge
- **⚠️ Use with extreme caution!**
```bash
git checkout main
git reset --hard <commit-before-merge>
git push --force origin main
```

**Scenario 3: Release merged to main, others have pulled**
- Use `git revert` to create a new commit that undoes the changes
- This preserves history and is safe for shared branches

## Best Practices

1. **Always test first** using `./gradlew testRelease`
2. **Review the release branch** before finalizing
3. **Use semantic versioning** for version numbers
4. **Create releases from release branches** only
5. **Verify the release** on GitHub after creation
6. **Announce the release** to users and stakeholders
7. **Keep release notes clear and concise**
8. **Include migration notes** for breaking changes
9. **Have a rollback plan** before releasing
10. **Test rollback procedures** in a safe environment