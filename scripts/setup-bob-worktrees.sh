#!/bin/sh
# Copyright 2026 IBM Corporation and others.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# SPDX-License-Identifier: Apache-2.0

set -e

# Die function for error handling
die() { echo "Error: $*" >&2; exit 1; }

echo "Setting up Bob Shell worktree..."

# Ensure we're in a git repository and change to root
git rev-parse --git-dir >/dev/null 2>&1 || die "Not in a git repository"
cd "$(git rev-parse --show-toplevel)"

# Check if .bob is already a registered worktree
git worktree list | grep -q "\.bob" && { echo "✓ Bob Shell worktree already set up"; exit 0; } || true

# Backup .bob if it exists and is not a worktree
[ ! -d ".bob" ] || {
    echo "Found existing .bob/ directory (not a worktree)"
    mkdir -p .bob-migration-backup
    echo "Merging .bob/ into .bob-migration-backup/"
    rsync -a .bob/ .bob-migration-backup/
    rm -rf .bob
    echo "✓ Backed up .bob/ to .bob-migration-backup/"
}

# Fetch the bob branch from origin if it doesn't exist locally
if ! git fetch origin bob:bob 2>&1; then
    # Check if local bob branch exists
    if git show-ref --verify --quiet refs/heads/bob; then
        echo "⚠ Warning: Could not update local bob branch from origin (likely diverged)" >&2
        echo "⚠ Using existing local bob branch" >&2
        echo "⚠ Please sync your bob branch:" >&2
        echo "    git checkout bob && git pull --rebase origin bob && git push origin bob && git checkout -" >&2
        echo "" >&2
    else
        die "bob branch not found on origin and does not exist locally"
    fi
fi

# Set up bob worktree
echo "Creating bob worktree..."
git worktree add .bob bob
echo "✓ bob worktree created at .bob/"

# Restore backed up content if it exists
[ ! -d ".bob-migration-backup" ] || {
    echo "Restoring backed up content..."
    rsync -av --exclude='.git' .bob-migration-backup/ .bob/
    echo "✓ Restored all content from backup"

    # Clean up the migration backup
    rm -rf .bob-migration-backup
    echo "✓ Removed .bob-migration-backup/"
    echo ""
    echo "Migration complete!"
}

# Create notes directory if it doesn't exist (not tracked in git)
[ ! -d .bob/notes ] || { mkdir -p .bob/notes; echo "✓ Created .bob/notes/ for personal notes (not tracked in git)"; }


echo ""
echo "Bob Shell worktree setup complete!"
echo ""
echo "Directory structure:"
printf "  %-24s (worktree → bob branch)\n" ".bob/"
# List contents, marking git-ignored ones
cd .bob
for item in *; do
    [ -e "$item" ] || continue
    git check-ignore -q "$item" 2>/dev/null && pattern="  %-30s (not tracked in git)\n" || pattern="  %s\n"
    printf "$pattern" "├── $(ls -Fd "$item")"
done
echo ""
echo "To update from remote:"
echo "  cd .bob && git pull"
echo ""
echo "To share changes:"
echo "  cd .bob"
echo "  git add <files>"
echo "  git commit -m \"your message\""
echo "  git push"
