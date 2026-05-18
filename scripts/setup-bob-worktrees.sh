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

echo "Setting up Bob Shell worktree..."

# Ensure we're in the repository root
if [ ! -d ".git" ]; then
    echo "Error: Must be run from the repository root directory"
    exit 1
fi

# Check if .bob is already a registered worktree
if git worktree list | grep -q "\.bob"; then
    echo "✓ Bob Shell worktree already set up"
    exit 0
fi

# Check if .bob directory exists but is not a worktree
if [ -d ".bob" ]; then
    echo "Found existing .bob/ directory (not a worktree)"
    echo "Migrating to worktree structure..."
    
    # Backup existing content
    echo "Backing up existing .bob/ content..."
    rm -rf .bob-migration-backup
    cp -r .bob .bob-migration-backup
    echo "✓ Backed up to .bob-migration-backup/"
    
    # Remove the directory
    rm -rf .bob
    echo "✓ Removed old .bob/ directory"
fi

# Fetch the bob branch if it doesn't exist locally
if ! git show-ref --verify --quiet refs/heads/bob; then
    if git show-ref --verify --quiet refs/remotes/origin/bob; then
        echo "Creating local bob branch from origin/bob..."
        git branch bob origin/bob
        echo "✓ Created local bob branch"
    else
        echo "Error: bob branch not found locally or on origin"
        echo "Please ensure the bob branch exists before running this script"
        exit 1
    fi
fi

# Set up bob worktree
echo "Creating bob worktree..."
git worktree add .bob bob
echo "✓ bob worktree created at .bob/"

# Restore backed up content if it exists
if [ -d ".bob-migration-backup" ]; then
    echo "Restoring backed up content..."
    echo ""
    
    # Copy everything except .git directory, with verbose output
    (cd .bob-migration-backup && find . -mindepth 1 -maxdepth 1 ! -name '.git' -print0) | while IFS= read -r -d '' item; do
        item_name=$(basename "$item")
        echo "  Restoring: $item_name"
        cp -r ".bob-migration-backup/$item_name" ".bob/" 2>/dev/null || true
    done
    
    echo ""
    echo "✓ Restored all content from backup"
    echo ""
    echo "Migration complete! All backed up content restored."
    echo "Original backup kept at: .bob-migration-backup/"
    echo "You can remove it after verifying: rm -rf .bob-migration-backup"
fi

# Create notes directory (not tracked in git)
if [ ! -d ".bob/notes" ]; then
    mkdir -p .bob/notes
    echo "✓ Created .bob/notes/ for personal notes (not tracked in git)"
fi

echo ""
echo "Bob Shell worktree setup complete!"
echo ""
echo "Directory structure:"
echo "  .bob/              (worktree → bob branch)"
echo "  ├── .gitignore     (ignores notes/)"
echo "  ├── config/        (Bob configuration)"
echo "  ├── docs/          (Shared documentation)"
echo "  ├── memory/        (Team memories)"
echo "  └── notes/         (Personal notes, not tracked)"
echo ""
echo "To update from remote:"
echo "  cd .bob && git pull"
echo ""
echo "To share changes:"
echo "  cd .bob"
echo "  git add <files>"
echo "  git commit -m \"your message\""
echo "  git push"