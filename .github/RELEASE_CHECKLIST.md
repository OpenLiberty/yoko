# Release Checklist for Yoko vX.Y.Z

Use this checklist to ensure a consistent and complete release process.

## Pre-Release Preparation

- [ ] All planned features and bug fixes are merged to main
- [ ] All tests passing on main branch: `./gradlew test`
- [ ] No critical bugs or security issues outstanding
- [ ] Dependencies reviewed and updated if needed
- [ ] Documentation reviewed and updated

## Environment Setup

- [ ] SDKMAN environment initialized: `sdk env`
- [ ] On main branch: `git checkout main`
- [ ] Latest changes pulled: `git pull origin main`
- [ ] Working directory clean: `git status`
- [ ] GitHub CLI authenticated: `gh auth status`

## Version Management

- [ ] Version bumped: `./gradlew bumpVersion`
  - Selected: [ ] Major [ ] Minor [ ] Patch
  - New version: `_______`
- [ ] Version change reviewed: `git diff gradle.properties`
- [ ] Version change committed:
  ```bash
  git add gradle.properties
  git commit -m "chore: bump version to X.Y.Z"
  git push origin main
  ```

## CHANGELOG Update

- [ ] CHANGELOG updated: `./gradlew updateChangelog`
- [ ] CHANGELOG changes reviewed: `git diff CHANGELOG.md`
- [ ] Release notes are clear and accurate
- [ ] Breaking changes clearly documented (if any)
- [ ] CHANGELOG committed:
  ```bash
  git add -p CHANGELOG.md
  git commit -m "chore: update CHANGELOG for vX.Y.Z"
  git push origin main
  ```

## Pre-Release Validation

- [ ] Release validation passed: `./gradlew validateRelease`
  - [ ] Version format correct (X.Y.Z)
  - [ ] CHANGELOG contains version
  - [ ] Git working directory clean
  - [ ] No existing tag
  - [ ] No existing GitHub release
  - [ ] GitHub CLI available
  - [ ] GitHub authenticated
  - [ ] Test results present

## Release Execution

- [ ] Release process started: `./gradlew release`
- [ ] Build completed successfully
- [ ] All artifacts generated (18 JARs + checksums)
- [ ] Distribution archive created
- [ ] Git tag created: `vX.Y.Z`
- [ ] GitHub release created
- [ ] All artifacts uploaded to GitHub

## Post-Release Verification

### GitHub Release
- [ ] Release visible at: https://github.com/OpenLiberty/yoko/releases/tag/vX.Y.Z
- [ ] Release title correct: "Yoko vX.Y.Z"
- [ ] Release notes complete and accurate
- [ ] All artifacts present:
  - [ ] 6 main JARs (yoko-osgi, yoko-util, yoko-spec-corba, yoko-rmi-spec, yoko-rmi-impl, yoko-core)
  - [ ] 6 sources JARs
  - [ ] 6 javadoc JARs
  - [ ] 18 SHA-256 checksums
  - [ ] 18 SHA-512 checksums
  - [ ] 1 distribution ZIP

### Artifact Verification
- [ ] Downloaded sample artifact for testing
- [ ] Checksum verification passed:
  ```bash
  sha256sum -c yoko-core-X.Y.Z.jar.sha256
  ```
- [ ] JAR file opens and contains expected classes

### Git Verification
- [ ] Tag fetched: `git fetch --tags`
- [ ] Tag exists locally: `git tag -l vX.Y.Z`
- [ ] Tag points to correct commit: `git show vX.Y.Z`
- [ ] Tag pushed to origin: `git ls-remote --tags origin | grep vX.Y.Z`

### Build Verification
- [ ] Clean build from tag works:
  ```bash
  git checkout vX.Y.Z
  ./gradlew clean build
  git checkout main
  ```

## Post-Release Tasks

### Documentation
- [ ] Documentation site updated (if needed)
- [ ] README.md updated (if needed)
- [ ] Migration guide created (if breaking changes)

### Communication
- [ ] Release announced to team
- [ ] Release notes shared (if applicable)
- [ ] Stakeholders notified

### Housekeeping
- [ ] Milestone closed (if applicable)
- [ ] Next milestone created
- [ ] Release issues closed
- [ ] Post-release issues created (if needed)

## Rollback Plan (if needed)

If issues are discovered after release:

- [ ] Assess severity (critical vs. minor)
- [ ] Decision made: [ ] Rollback [ ] Hotfix [ ] Document known issue

### If Rollback Required:
- [ ] GitHub release deleted: `gh release delete vX.Y.Z --yes`
- [ ] Git tag deleted locally: `git tag -d vX.Y.Z`
- [ ] Git tag deleted remotely: `git push origin :refs/tags/vX.Y.Z`
- [ ] Version reverted in gradle.properties
- [ ] CHANGELOG updated to remove release entry
- [ ] Stakeholders notified of rollback

## Notes

**Release Date:** _______________________

**Released By:** _______________________

**Issues/Observations:**
- 
- 
- 

**Follow-up Actions:**
- 
- 
- 

---

**Checklist Version:** 1.0  
**Last Updated:** 2026-04-16
