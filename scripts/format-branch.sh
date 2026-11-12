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

# Enforce top-level subshell to avoid leaking environment changes (in case script is sourced)
(
  # Stop on first unexpected error
  set -e
  # Disable globbing
  set -f

  die() {
    echo "$@" >&2
    exit 1
  }

  usage() {
    echo "usage: $0 [--base rev] [--branch name] [--tool-dir dir] [--keep]"
    echo "  Copy the current branch to a new branch in which the Yoko formatting has been applied cleanly."
    echo "  The new branch consists of:"
    echo "    1. one 'style:' commit, on the merge base, that formats every file the branch touches;"
    echo "    2. a rebuild of each of the branch's commits (same author, date and message): the files that commit changes"
    echo "       are taken verbatim from it and formatted, and everything else carries over from the previous level,"
    echo "       so that every commit shows only its real change and the branch stays reviewable."
    echo "  The current branch and working tree are left alone; all the work is done in a temporary worktree."
    echo "  -b,--base rev    the branch point (default: origin/dev, or dev if there is no origin/dev)"
    echo "  -n,--branch n    the name of the new branch (default: <current branch>-formatted)"
    echo "  -t,--tool-dir d  take the formatter from d (default: the checkout containing this script)"
    echo "  -k,--keep        keep the temporary worktree (for debugging)"
    echo "  -h,--help        print this usage info"
  }

  base=
  new_branch=
  keep=
  tool_dir="$(cd "$(dirname "$0")/.." && pwd)"

  while [ $# -gt 0 ]; do
    case "$1" in
      -h|--help) usage; exit 0 ;;
      -b|--base) shift; [ $# -gt 0 ] || die "--base needs an argument"; base="$1" ;;
      -n|--branch) shift; [ $# -gt 0 ] || die "--branch needs an argument"; new_branch="$1" ;;
      -t|--tool-dir) shift; [ $# -gt 0 ] || die "--tool-dir needs an argument"; tool_dir="$(cd "$1" && pwd)" ;;
      -k|--keep) keep=true ;;
      *) usage >&2; die "Unknown argument: $1" ;;
    esac
    shift
  done

  command -v git > /dev/null 2>&1 || die "Can not find 'git' command."
  [ -x "$tool_dir/scripts/yoko-format.sh" ] || die "No formatter found in $tool_dir"

  branch="$(git symbolic-ref --short HEAD)" || die "HEAD is not on a branch"
  if [ -z "$base" ]; then
    if git rev-parse --verify -q origin/dev > /dev/null; then base=origin/dev; else base=dev; fi
  fi
  [ -n "$new_branch" ] || new_branch="$branch-formatted"
  git rev-parse --verify -q "refs/heads/$new_branch" > /dev/null && die "Branch $new_branch already exists"

  merge_base="$(git merge-base "$base" HEAD)" || die "No common ancestor of $base and HEAD"
  [ -z "$(git rev-list --merges "$merge_base..HEAD")" ] || die "$branch contains merge commits; rebase it onto $base first"
  commits="$(git rev-list --reverse "$merge_base..HEAD")"
  [ -n "$commits" ] || die "$branch has no commits beyond $base"

  tmp="$(mktemp -d "${TMPDIR:-/tmp}/yoko-format-branch.XXXXXX")"
  wt="$tmp/wt"
  touched="$tmp/touched"
  changed="$tmp/changed"
  present="$tmp/present"
  cleanup() {
    git worktree remove --force "$wt" > /dev/null 2>&1 || true
    git worktree prune
    [ -n "$keep" ] && echo "Kept $tmp" || rm -rf "$tmp"
  }
  trap cleanup EXIT

  # Every file the branch adds, changes, deletes or renames (--no-renames lists both names of a rename)
  git diff --no-renames --name-only "$merge_base" HEAD > "$touched"

  git worktree add -q --detach "$wt" "$merge_base"

  # Format the tree of the worktree, then undo changes to all files not listed in the file named by $1
  # (the formatter can not be restricted to some files, but the index holds the version to go back to)
  format_listed() {
    "$tool_dir/scripts/yoko-format.sh" --tool-dir "$tool_dir" "$wt" > /dev/null
    git -C "$wt" diff --name-only | grep -Fvx -f "$1" | tr '\n' '\0' | xargs -0 git -C "$wt" checkout -q -- 2> /dev/null || true
  }

  # Use the tree of the worktree, as it stands, as the content of a new commit
  tree_of_worktree() {
    git -C "$wt" add -A
    git -C "$wt" write-tree
  }

  echo "Formatting the files touched by $branch on the merge base ($merge_base)"
  format_listed "$touched"
  tree="$(tree_of_worktree)"
  if [ "$tree" = "$(git rev-parse "$merge_base^{tree}")" ]; then
    prev="$merge_base"
    echo "  (nothing to reformat)"
  else
    prev="$(git -c commit.gpgsign=false commit-tree "$tree" -p "$merge_base" -m "style: format the files touched by $branch")"
  fi

  count=0
  total="$(echo "$commits" | wc -l | tr -d ' ')"
  for c in $commits; do
    count=$((count + 1))
    echo "[$count/$total] $(git log -1 --format='%h %s' "$c")"
    # Start from the previous level and bring in only the files this commit changes
    git -C "$wt" checkout -q -f --detach "$prev"
    git diff --no-renames --name-only "$c^" "$c" > "$changed"
    git diff --no-renames --name-only --diff-filter=d "$c^" "$c" > "$present"
    # (an empty path list would make checkout switch to $c)
    if [ -s "$present" ]; then
      tr '\n' '\0' < "$present" | xargs -0 git -C "$wt" checkout -q "$c" --
    fi
    git diff --no-renames --name-only --diff-filter=D "$c^" "$c" | while IFS= read -r f; do
      rm -f "$wt/$f"
    done
    git -C "$wt" add -A
    format_listed "$changed"
    tree="$(tree_of_worktree)"
    # (env keeps the author settings from leaking into later commands)
    prev="$(git log -1 --format=%B "$c" | env \
      GIT_AUTHOR_NAME="$(git log -1 --format=%an "$c")" \
      GIT_AUTHOR_EMAIL="$(git log -1 --format=%ae "$c")" \
      GIT_AUTHOR_DATE="$(git log -1 --format=%aI "$c")" \
      git -c commit.gpgsign=false commit-tree "$tree" -p "$prev" -F -)"
  done

  git branch "$new_branch" "$prev"
  echo "✓ Created $new_branch from $branch"
  echo "  Compare the result: git diff $branch $new_branch --stat"
)
