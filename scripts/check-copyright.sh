#!/bin/sh

# Copyright 2025 IBM Corporation and others.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an \"AS IS\" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# SPDX-License-Identifier: Apache-2.0

# Enforce top-level subshell to avoid leaking environment changes (in case script is sourced)
(
  # Stop on first unexpected error
  set -e

  usage() {
    echo "usage:\t$0 [-q|--quiet|-t|--terse|-v|--verbose|--staged] git-base-ref"
    echo "\t-h,--help\tprint this usage info"
    echo "\t-t,--terse\tprint only the failing file paths"
    echo "\t-q,--quiet\tsuppress all non-error output"
    echo "\t-v,--verbose\tenable verbose output"
    echo "\t-s,--staged\tcheck only git staged files"
  }

  # Copy stdout and stderr to other file descriptors for logging and error reporting
  exec 3>/dev/null 4>&1 5>&1 6>&2
  # Define semantic functions to echo or cat messages to the various file descriptors
  echocat() { { [ $# -gt 0 ] && echo "$@"; } || cat; }
  log() { echocat "$@" >&3; }
  inf() { echocat "$@" >&4; }
  wrn() { echocat "$@" >&5; }
  err() { echocat "$@" >&6; }
  # Define a Perl-style "die" function to emit an error message and exit with an error code
  die() {
    err "$@"
    exit 1
  }

  # Flag to indicate if we're checking only staged files
  CHECK_STAGED=0

  # Parse script options
  while [ $# -gt 0 ]; do
    case "$1" in
      # -h print a usage message and exits successfully
      -h|--help)
        usage
        exit 0
        ;;
      # -t disable logging and info
      # print only last arg to warning
      # (this should be just the pathname to make it easy to open in an editor)
      -t|--terse)
        exec 3>/dev/null 4>/dev/null 5>&1
        wrn() { [ $# -le 1 ] || shift $(($#-1)); echocat "$@" >&5; }
        shift
        ;;
      # -q disables logging, info, and warning
      -q|--quiet)
        exec 3>/dev/null 4>/dev/null 5>/dev/null;
        shift
        ;;
      # -v enables logging
      -v|--verbose)
        exec 3>&1        4>&1        5>&1
        shift
        ;;
      # -s or --staged checks only git staged files
      -s|--staged)
        CHECK_STAGED=1
        shift
        ;;
      # -- indicates the explicit end of options, so consume it and exit the loop
      --)
        shift
        break
        ;;
      # any other option-like string is an error
      # print error and usage and exit with an error code
      -*)
        err "$0: unknown option '$1'";
        usage | die
        ;;
      # any non-option-like string indicates the end of the options
      *) break;;
    esac
  done

  # Check that git is a command
  command -v git > /dev/null 2>&1 || die "Can not find 'git' command."

  # Handle base ref requirement based on mode
  if [ $CHECK_STAGED -eq 1 ]; then
    # Base ref is not required when checking only staged files
    BASE=""
    inf "Checking only staged files..."
  else
    # Check that base ref has been specified
    [ -n "$1" ] || die "Missing required first parameter: git-base-ref"
    BASE="$1"

    # Check it is a valid ref
    git rev-parse --quiet --verify "$BASE" > /dev/null || die "Specified git base ref '$BASE' is not a valid ref in this repository."
  fi

  # Git uses the following one-character change status codes
  # A - Added
  # C - Copied
  # D - Deleted
  # M - Modified
  # R - Renamed

  # Keep track of how many files failed the copyright check so we can find them all and return 0 if there were none
  FAILED=0

  # Get the current year once for the entire script
  CURRENT_YEAR="$(date +%Y)"

  # Set source reference based on mode
  SOURCE_REF="${CHECK_STAGED:+HEAD}"
  SOURCE_REF="${SOURCE_REF:-$BASE}"

  # Look for unsupported changes with Broken (B), changed (T), Unmerged (U), or Unknown (X) status
  BAD_FILES="$(git diff ${CHECK_STAGED:+--staged} --name-status --diff-filter=BTUX $SOURCE_REF)"

  # Log deleted files
  git diff ${CHECK_STAGED:+--staged} --name-only --diff-filter=D $SOURCE_REF | sed 's/^/🫥 Ignoring deleted file: /' | log

  [ -z "$BAD_FILES" ] || {
    err "‼️ This script ($0) may need fixing to deal with more types of change."
    echo "$BAD_FILES" | sed 's/^/🤯 Unsupported change type: /' | err
    FAILED=$(( FAILED + $(echo "$BAD_FILES"|wc -l) ))
  }

  # Define how to compare a file against its origin for significant content changes.
  # Succeed if there are differences, and print the filename.
  isReallyDifferent() { ! git diff --ignore-all-space --quiet "$1" "$2" 2>/dev/null && echo "$2"; }

  # Read status, source, and destination as separate records (lines).
  # Check the status is R... or C... (otherwise it was parsed incorrectly).
  # Then compare the source and dest for significant differences.
  # Lastly, if they were different, check them for copyright.
  ignoreCopiesAndRenames() {
    while read status && read src && read dst
    do
      case "$status" in
        R100) log "🫥 Ignoring renamed file: $src -> $dst" ;;
        C100) log "🫥 Ignoring copied file: $src -> $dst" ;;
        R*) isReallyDifferent "${SOURCE_REF}:$src" "$dst" || log "🫥 Ignoring renamed file: $src -> $dst" ;;
        C*) isReallyDifferent "${SOURCE_REF}:$src" "$dst" || log "🫥 Ignoring copied file: $src -> $dst" ;;
        *) die "Unexpected status while parsing git diff output: status='$status' src='$src' dst='$dst'" ;;
      esac
    done
  }

  # Function to print each pathname from stdin to stdout unless it has good copyright
  reportBadCopyright() {
    while read filePath; do
      [ -f "$filePath" ] || die "Cannot check copyright in non-existent file: '$filePath'"
      # ignore markdown files
      [ "${filePath%.md}" == "$filePath" ] || {
        log "🫥 Ignoring markdown file: $filePath"
        continue
      }

      # Get file content based on mode
      if [ $CHECK_STAGED -eq 1 ]; then
        # For staged files, check the staged version
        fileContent=$(git show ":$filePath")
        # For staged files, always use the current year
        yearModified="$CURRENT_YEAR"
      else
        # For non-staged files, check the working copy
        fileContent=$(cat "$filePath")
        # Get the year from git history, or use current year if file has uncommitted changes
        yearModified=$(git log -1 --pretty=format:%cd --date=format:%Y -- "$filePath")
        git diff --quiet HEAD -- "$filePath" 2> /dev/null || yearModified="$CURRENT_YEAR"
      fi

      # Check for a license identifier
      echo "$fileContent" | grep -Eq "SPDX-License-Identifier: Apache-2.0" || {
        wrn "👿 License identifier not found:" "$filePath"
        echo "$filePath"
        continue
      }

      # Check for correct copyright year
      if echo "$fileContent" | grep -q "Copyright $yearModified"; then
        inf "😅 Copyright OK: $filePath"
      else
        # Extract the year from the copyright statement using sed
        existingModifiedYear="$(echo "$fileContent" | grep -Eaom 1 'Copyright [0-9]+' | cut -d ' ' -f 2)"
        case "$existingModifiedYear" in
          "$yearModified") continue ;;
          "")              wrn "🤬 No copyright year (expected '$yearModified'):" "$filePath" ;;
          *)               wrn "😡 Wrong copyright year (expected '$yearModified' but was '$existingModifiedYear'):" "$filePath" ;;
        esac
        echo "$filePath"
      fi
    done
  }

  # Renamed (R) and copied (C) files are more complicated.
  # They can report as less than 100% identical even when their contents are the same.
  # This is apparently due to metadata changes. Shrug.
  # So check whether the contents have changed significantly.

  # Check for added and modified files
  if [ $CHECK_STAGED -eq 1 ]; then
    inf "Checking staged added and modified files..."
  else
    inf "Checking added and modified files..."
  fi

  FAILED=$((FAILED + $(git diff ${CHECK_STAGED:+--staged} --name-only --find-copies-harder --diff-filter=AM $SOURCE_REF | reportBadCopyright | wc -l)))

  # Check for renamed and copied files
  if [ $CHECK_STAGED -eq 1 ]; then
    inf "Checking staged renamed and copied files..."
  else
    inf "Checking renamed and copied files..."
  fi

  OUTPUT="$(git diff ${CHECK_STAGED:+--staged} --name-status --find-copies-harder --diff-filter=CR -z $SOURCE_REF 2>/dev/null | tr '\0' '\n')"
  FAILED=$((FAILED + $(echo "$OUTPUT"| ignoreCopiesAndRenames | reportBadCopyright | wc -l)))
  exit $FAILED
)
