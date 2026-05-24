---
name: Shell Scripting
description: Expert knowledge in writing clean, maintainable, and Bourne shell-compliant scripts
version: 1.0.0
tags:
  - shell
  - scripting
  - bash
  - sh
  - automation
modes:
  - code
  - advanced
---

# Shell Scripting Skill

## Description
Expert knowledge in writing clean, maintainable, and Bourne shell-compliant scripts following best practices.

## When to Use
- Writing new shell scripts
- Modifying existing shell scripts
- Reviewing shell script changes
- Debugging shell script issues

## Core Principles

### 1. Bourne Shell Compliance
- Use `#!/bin/sh` shebang
- Avoid bash-specific features
- Test with `sh -n script.sh`
- Compatible with dash, ash, and minimal shells

### 2. Error Handling
Define and use a `die()` function:
```sh
die() { echo "Error: $*" >&2; exit 1; }
```

Always use `set -e` and `die` for errors:
```sh
set -e
command || die "command failed"
```

### 3. One-Liner Patterns

Git operations:
```sh
git rev-parse --git-dir >/dev/null 2>&1 || die "Not in a git repository"
cd "$(git rev-parse --show-toplevel)"
git worktree list | grep -q "pattern" && { echo "exists"; exit 0; } || true
```

Conditional execution:
```sh
[ ! -d "path" ] || { actions; }
command && action || alternative
```

### 4. File Operations

Use rsync for copying/merging:
```sh
rsync -a source/ dest/              # Archive mode
rsync -av --exclude='.git' src/ dst/  # Verbose with exclusions
```

### 5. Code Style

- No trailing whitespace
- Quote variables: `"$var"`
- One-liner functions: `func() { command; }`
- Errors to stderr: `echo "Error" >&2`

## Testing Requirements

Test all scenarios:
1. Clean slate (nothing present)
2. Existing data present
3. Existing backup present
4. Both data and backup present

Validate syntax:
```sh
sh -n script.sh
```

## Anti-Patterns

Avoid:
- bash-specific features (`[[`, `=~`, arrays)
- `echo -e` (use `printf`)
- `source` (use `.`)
- Trailing whitespace
- Unquoted variables

## Example Template

```sh
#!/bin/sh
# Copyright 2026 IBM Corporation and others.
# SPDX-License-Identifier: Apache-2.0

set -e

die() { echo "Error: $*" >&2; exit 1; }

echo "Starting script..."

# Check prerequisites
git rev-parse --git-dir >/dev/null 2>&1 || die "Not in a git repository"
cd "$(git rev-parse --show-toplevel)"

# Main logic
[ ! -d "source" ] || {
    echo "Processing source..."
    rsync -a source/ backup/
    rm -rf source
}

command || die "command failed"

echo "✓ Script complete"
```

## Reference
See `.bob/modes/shell-scripting.md` for comprehensive guide with detailed examples and patterns.