# Quick Start: Creating a Yoko Release

This is a quick reference for creating releases. For detailed information, see [RELEASE_PROCESS.md](RELEASE_PROCESS.md).

## Prerequisites Checklist

- [ ] Java 11+ installed
- [ ] git-cliff installed (optional, for CHANGELOG generation)
- [ ] GitHub CLI (`gh`) installed and authenticated
- [ ] Clean git working directory
- [ ] All tests passing
- [ ] CHANGELOG.md updated with new version

## Quick Release Steps

### 1. Update CHANGELOG.md

**Option A: Using git-cliff (Recommended)**
```bash
./gradlew updateChangelog
# or for specific tag
./gradlew updateChangelogForTag -PreleaseTag=v1.5.3
```

**Option B: Manual Update**
```markdown
## [v1.5.3] - 2026-01-08

### 🚀 Features
- Your feature here

### 🐛 Bug Fixes
- Your fix here
```

### 2. Commit and Push

```bash
git add CHANGELOG.md
git commit -m "chore: prepare release v1.5.3"
git push origin main
```

### 3. Create Release (Choose One)

#### Option A: Automated (Recommended)
```bash
git tag v1.5.3
git push origin v1.5.3
```
✅ GitHub Actions handles everything automatically

#### Option B: Manual via Gradle
```bash
./gradlew release
```
✅ Creates release from your local machine

#### Option C: Manual via GitHub UI
1. Go to Actions → Release workflow
2. Click "Run workflow"
3. Enter version: `v1.5.3`
4. Click "Run workflow"

## Verify Release

Visit: https://github.com/OpenLiberty/yoko/releases

Check for:
- ✅ Correct version number
- ✅ Release notes
- ✅ All JAR files
- ✅ Checksums (.sha256, .sha512)
- ✅ Distribution archive

## Common Commands

```bash
# Prepare release branch (creates branch, updates CHANGELOG)
./gradlew prepareReleaseBranch -PreleaseVersion=1.5.3

# Finalize release (tag, merge to main, cleanup)
./gradlew finalizeRelease -PreleaseVersion=1.5.3

# Update CHANGELOG with git-cliff (manual)
./gradlew updateChangelog
./gradlew updateChangelogForTag -PreleaseTag=v1.5.3

# Verify prerequisites
./gradlew verifyReleasePrerequisites

# Build artifacts only
./gradlew assembleRelease

# Generate release notes only
./gradlew generateReleaseNotes

# Create GitHub release
./gradlew createGitHubRelease
```

## Troubleshooting

| Problem | Solution |
|---------|----------|
| `git-cliff not found` | Install: `brew install git-cliff` (macOS) or see https://git-cliff.org/docs/installation |
| `gh not found` | Install: `brew install gh` (macOS) or `sudo apt install gh` (Linux) |
| `Not authenticated` | Run: `gh auth login` |
| `Dirty working directory` | Commit or stash changes |
| `No version in CHANGELOG` | Run `./gradlew updateChangelog` or add manually: `## [v1.5.3] - 2026-01-08` |

## Need Help?

See [RELEASE_PROCESS.md](RELEASE_PROCESS.md) for detailed documentation.

## Testing Before Release

**Always test first!**

```bash
# Dry-run test (makes no changes)
./gradlew testRelease -PreleaseVersion=1.5.3
```

This checks:
- Git status and prerequisites
- CHANGELOG preview
- Build success
- No actual changes made

## Rolling Back a Release

If something goes wrong:

```bash
# Quick rollback (deletes tag, release, branch)
./gradlew rollbackRelease -PreleaseVersion=1.5.3

# Keep branch for investigation
./gradlew rollbackRelease -PreleaseVersion=1.5.3 -PkeepBranch=true
```

**If already merged to main:**
```bash
# Revert the merge commit
git checkout main
git revert -m 1 <merge-commit-hash>
git push origin main
```