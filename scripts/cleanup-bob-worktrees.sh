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

echo "Cleaning up Bob Shell worktree..."

# Ensure we're in the repository root
if [ ! -d ".git" ]; then
    echo "Error: Must be run from the repository root directory"
    exit 1
fi

# First, prune any orphaned worktree entries
git worktree prune 2>/dev/null || true

# Check if .bob worktree is registered
if git worktree list | grep -q "\.bob"; then
    echo "Removing registered bob worktree..."

    # Backup notes if they exist and directory is accessible
    if [ -d ".bob/notes" ] && [ "$(ls -A .bob/notes 2>/dev/null)" ]; then
        echo "Backing up .bob/notes/..."
        rm -rf .bob-notes-backup
        cp -r .bob/notes .bob-notes-backup
        echo "✓ Backed up to .bob-notes-backup/"
    fi

    # Remove the worktree (force in case directory is missing)
    git worktree remove .bob --force 2>/dev/null || true
    echo "✓ Removed bob worktree"

    # Show backup message if notes were backed up
    if [ -d ".bob-notes-backup" ]; then
        echo ""
        echo "Personal notes backed up to: .bob-notes-backup/"
        echo "To restore after running setup script:"
        echo "  cp -r .bob-notes-backup .bob/notes"
        echo "  rm -rf .bob-notes-backup"
    fi
elif [ -d ".bob" ]; then
    # Directory exists but not registered as worktree
    echo "Found .bob/ directory but not registered as worktree"
    echo "Removing directory..."

    # Backup notes if they exist
    if [ -d ".bob/notes" ] && [ "$(ls -A .bob/notes 2>/dev/null)" ]; then
        echo "Backing up .bob/notes/..."
        rm -rf .bob-notes-backup
        cp -r .bob/notes .bob-notes-backup
        echo "✓ Backed up to .bob-notes-backup/"
    fi

    rm -rf .bob
    echo "✓ Removed .bob/ directory"
else
    echo "✓ bob worktree not found (already removed)"
fi

# Final prune to clean up any remaining references
git worktree prune 2>/dev/null || true

echo ""
echo "Bob Shell worktree cleanup complete!"
echo ""
echo "To set up worktree again, run: ./scripts/setup-bob-worktrees.sh"
