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

# Check if worktree already exists
if [ -d ".bob" ]; then
    echo "Warning: .bob/ directory already exists. Skipping setup..."
    echo "✓ Bob Shell worktree already set up"
    exit 0
fi

# Set up bob worktree
echo "Creating bob worktree..."
git worktree add .bob bob
echo "✓ bob worktree created at .bob/"

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