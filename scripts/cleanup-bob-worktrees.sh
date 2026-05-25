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

die() { echo "Error: $*" >&2; exit 1; }

echo "Cleaning up Bob Shell worktree..."

# Ensure we're in a git repository and change to root
git rev-parse --git-dir >/dev/null 2>&1 || die "Not in a git repository"
cd "$(git rev-parse --show-toplevel)"

# Prune any orphaned worktree entries
git worktree prune 2>/dev/null || true

# Check if .bob is a registered worktree
git worktree list | grep -q "\.bob" || { echo "✓ Bob Shell worktree not found (nothing to clean up)"; exit 0; }

# Check for uncommitted changes
git -C .bob diff-index --quiet HEAD -- 2>/dev/null || die "You have uncommitted changes in .bob/. Please commit or stash them first."

BACKUP_DIR="$PWD/.bob-migration-backup"
[ -d "$BACKUP_DIR" ] || { echo "Creating backup directory at $BACKUP_DIR"; mkdir -p "$BACKUP_DIR"; }

# Change to .bob worktree
cd .bob

# Backup git-ignored content from .bob before cleanup
echo "Backing up git-ignored content in .bob ..."

# backup ignored files
git ls-files --others --ignored --exclude-standard | rsync -avR --files-from=- . "$BACKUP_DIR/"

# remove the git worktree
cd ..
git worktree remove .bob

# restore the backup
mv .bob-migration-backup/ .bob 

echo ""
echo "Bob Shell worktree cleanup complete!"
echo ""
echo "To set up worktree again, run: scripts/setup-bob-worktrees.sh"
