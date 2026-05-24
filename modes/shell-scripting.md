# Shell Scripting Mode

## Overview
Mode for writing clean, maintainable, and Bourne shell-compliant scripts following best practices discovered during the yoko project.

## Core Principles

### 1. Bourne Shell Compliance
- Use `#!/bin/sh` shebang
- Avoid bash-specific features
- Test with `sh -n script.sh` for syntax validation
- Compatible with dash, ash, and other minimal shells

### 2. Error Handling
- Always use `set -e` to exit on errors
- Define a `die()` function for consistent error handling:
  ```sh
  die() { echo "Error: $*" >&2; exit 1; }
  ```
- Use `die "message"` instead of `{ echo "Error: ..."; exit 1; }`
- Send errors to stderr with `>&2`

### 3. One-Liner Patterns

#### Conditional Execution
```sh
# Test and execute on success
command && { action; } || true

# Test and execute on failure
command || die "error message"

# Test directory/file existence
[ ! -d "path" ] || { actions; }
[ -f "file" ] && action || alternative
```

#### Git Operations
```sh
# Check if in git repo
git rev-parse --git-dir >/dev/null 2>&1 || die "Not in a git repository"

# Navigate to repo root
cd "$(git rev-parse --show-toplevel)"

# Check for specific branch/worktree
git worktree list | grep -q "pattern" && { echo "exists"; exit 0; } || true
```

### 4. File Operations

#### Use rsync for Copying/Merging
```sh
# Archive mode preserves everything
rsync -a source/ dest/

# Verbose with exclusions
rsync -av --exclude='.git' source/ dest/

# Merges intelligently, handles all file types
```

#### Directory Navigation
```sh
# Use subshells when you need to return
(cd dir && commands)

# Or just cd without returning if at end of script
cd dir
for item in *; do
    # work with items
done
```

### 5. Clean Code Style

#### No Trailing Whitespace
- Always remove trailing spaces
- Use consistent indentation (4 spaces)

#### Function Declarations
```sh
# One-liner for simple functions
func() { command; }

# Multi-line for complex functions
func() {
    command1
    command2
}
```

#### Variable Quoting
```sh
# Always quote variables
cd "$directory"
echo "$message"

# Exception: when you want word splitting
for item in $list; do
```

### 6. User Feedback

#### Progress Messages
```sh
echo "Starting operation..."
echo "✓ Operation complete"
echo "✗ Operation failed"
```

#### Verbose Operations
```sh
# Show what's happening
rsync -av source/ dest/  # -v for verbose

# List with details
ls -Fd item  # Shows file type indicators
```

### 7. Testing Patterns

#### Test All Scenarios
1. Nothing present (clean slate)
2. Existing data present
3. Existing backup present
4. Both data and backup present

#### Validation
```sh
# Syntax check
sh -n script.sh

# Test in subshell
(cd /tmp && /path/to/script.sh)
```

## Common Patterns

### Backup and Restore
```sh
# Backup with merge
[ ! -d "source" ] || {
    mkdir -p backup
    rsync -a source/ backup/
    rm -rf source
}

# Restore
[ ! -d "backup" ] || {
    rsync -av --exclude='.git' backup/ target/
    rm -rf backup
}
```

### Git Branch Management
```sh
# Fetch branch
git fetch origin branch:branch || die "branch not found on origin"

# Create worktree
git worktree add path branch
```

### Directory Listing with Git Status
```sh
cd directory
for item in *; do
    [ -e "$item" ] || continue
    git check-ignore -q "$item" 2>/dev/null && \
        pattern="  %-30s (ignored)\n" || \
        pattern="  %s\n"
    printf "$pattern" "├── $(ls -Fd "$item")"
done
```

## Anti-Patterns to Avoid

### Don't Use
- `bash`-specific features (`[[`, `=~`, arrays)
- `echo -e` (not portable, use `printf`)
- `source` (use `.` instead)
- Trailing whitespace
- Multiple `cd ..` to return (use subshells or don't return)

### Don't Do
```sh
# Bad: separate rm and mv
rm -rf backup
mv source backup

# Good: use rsync to merge
rsync -a source/ backup/
rm -rf source
```

```sh
# Bad: verbose error handling
if ! command; then
    echo "Error: message"
    exit 1
fi

# Good: use die function
command || die "message"
```

## Checklist for Shell Scripts

- [ ] Uses `#!/bin/sh` shebang
- [ ] Has `set -e` for error handling
- [ ] Defines and uses `die()` function
- [ ] Uses one-liners where appropriate
- [ ] Uses `rsync` for file operations
- [ ] No trailing whitespace
- [ ] Properly quotes variables
- [ ] Sends errors to stderr
- [ ] Provides user feedback
- [ ] Tested with `sh -n`
- [ ] Tested all scenarios
- [ ] Bourne shell compliant

## Example Script Template

```sh
#!/bin/sh
# Copyright and license header

set -e

# Die function for error handling
die() { echo "Error: $*" >&2; exit 1; }

echo "Starting script..."

# Check prerequisites
git rev-parse --git-dir >/dev/null 2>&1 || die "Not in a git repository"
cd "$(git rev-parse --show-toplevel)"

# Main logic with one-liners
[ ! -d "source" ] || {
    echo "Processing source..."
    rsync -a source/ backup/
    rm -rf source
}

# More operations
command || die "command failed"

echo "✓ Script complete"