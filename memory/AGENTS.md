## Bob-Shell Team Memories

This file contains shared team knowledge and preferences for the Yoko project.

### Git Commit Conventions
- All git commits should follow the Conventional Commits specification (https://www.conventionalcommits.org/)
- Commit message format: `type(scope): description`
- Common types: feat, fix, docs, style, refactor, test, chore, build, ci

### Release Tagging
- Release tags MUST be **annotated** (not lightweight)
- Message template: `Release vX.Y.Z: see CHANGELOG.md for details`
- Full command: `git tag -a vX.Y.Z -m "Release vX.Y.Z: see CHANGELOG.md for details"`
- A lightweight tag (`git tag vX.Y.Z`) is WRONG — it creates a `commit` object, not a `tag` object
- Verify a tag is annotated: `git cat-file -t vX.Y.Z` must return `tag`, not `commit`
- Historical note: `v1.6.2` was accidentally created as a lightweight tag — all other releases (v1.0.0–v1.6.1) are correctly annotated
- When helping with a release, Bob MUST use `git tag -a` with the exact message template above and MUST verify with `git cat-file -t` before pushing

### Git Commit Attribution for AI-Generated Content
- When committing content generated or significantly modified by AI tools, add attribution at the end of the commit message body
- Format: `Co-authored-by-AI: <Tool Name> <Version> (<LLM and Version>)`
- Version number is **mandatory** - developer must supply it if not automatically discoverable
- Include LLM and version in parentheses when known
- Examples:
  - `Co-authored-by-AI: IBM Bob 1.0.1 (Claude 3.5 Sonnet)`
  - `Co-authored-by-AI: IBM Bob 1.0.1 (GPT-4)`
  - `Co-authored-by-AI: GitHub Copilot 1.2.3 (GPT-4)`
- Note: Both Bob Shell and Bob IDE can be attributed as "IBM Bob"

### Project-Specific Notes
- Add project-specific conventions and preferences here as the team discovers them

### License Headers
- **Copyright Year**: MUST use current year (2026) or range ending in current year (e.g., "2023-2026")
- **Required Header**: Apache 2.0 with "IBM Corporation and others" as copyright holder
- **SPDX Identifier**: Must include `SPDX-License-Identifier: Apache-2.0` at end of header
- **Check Script**: Run `scripts/check-copyright.sh` to validate headers before commit
- **Bob Enforcement**: When creating or modifying Java files, Bob MUST ensure copyright year includes current year

### Code Review Checklist
When reviewing code changes, Bob MUST verify:
1. All modified `.java` files have copyright headers with current year (2026)
2. New `.java` files have complete Apache 2.0 headers
3. Run `scripts/check-copyright.sh` to validate compliance
