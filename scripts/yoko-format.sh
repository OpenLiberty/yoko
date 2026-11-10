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
    echo "usage: $0 [--check] [--tool-dir dir] [--recipe name] [dir]"
    echo "  Format the Java source in dir (default: the current git checkout) using the Yoko OpenRewrite recipes."
    echo "  Works on any checkout, however old, because it injects the formatter with a Gradle init script."
    echo "  -c,--check       change nothing; exit 1 and list the files that would change"
    echo "  -t,--tool-dir d  take the formatter from d (a checkout of this repo; default: the one containing this script)"
    echo "  -r,--recipe r    run recipe r instead of yoko.format.All"
    echo "  -h,--help        print this usage info"
  }

  mode=rewriteRun
  recipe=yoko.format.All
  tool_dir="$(cd "$(dirname "$0")/.." && pwd)"
  target=

  while [ $# -gt 0 ]; do
    case "$1" in
      -h|--help) usage; exit 0 ;;
      -c|--check) mode=rewriteDryRun ;;
      -t|--tool-dir) shift; [ $# -gt 0 ] || die "--tool-dir needs an argument"; tool_dir="$(cd "$1" && pwd)" ;;
      -r|--recipe) shift; [ $# -gt 0 ] || die "--recipe needs an argument"; recipe="$1" ;;
      -*) usage >&2; die "Unknown option: $1" ;;
      *) [ -z "$target" ] || die "Only one directory may be given"; target="$(cd "$1" && pwd)" ;;
    esac
    shift
  done

  [ -n "$target" ] || target="$(git rev-parse --show-toplevel)"
  [ -f "$tool_dir/scripts/format/init.gradle" ] || die "No formatter found in $tool_dir"
  [ -f "$target/gradlew" ] || die "$target does not look like a Yoko checkout (no gradlew)"

  tmp="${TMPDIR:-/tmp}/yoko-format.$$"

  # The formatter needs Java 17 (Gradle 7.5.1 is the build's version, and OpenRewrite needs 11+)
  if [ -z "$YOKO_FORMAT_JAVA_HOME" ]; then
    for j in "$HOME"/.sdkman/candidates/java/17.*; do [ -x "$j/bin/java" ] && YOKO_FORMAT_JAVA_HOME="$j"; done
  fi
  [ -z "$YOKO_FORMAT_JAVA_HOME" ] || JAVA_HOME="$YOKO_FORMAT_JAVA_HOME"
  export JAVA_HOME

  # Build the recipes once per tool checkout (Gradle will skip this if nothing changed)
  recipe_build="$tool_dir/build-tools/yoko-rewrite"
  [ -d "$recipe_build" ] || die "No $recipe_build"
  (cd "$recipe_build" && "$tool_dir/gradlew" -q --console=plain jar) || die "Could not build the Yoko OpenRewrite recipes"
  # (globbing is disabled above, so the jar name is spelled out; keep in sync with build-tools/yoko-rewrite/build.gradle)
  recipe_jar="$recipe_build/build/libs/yoko-rewrite-1.0.0.jar"
  [ -f "$recipe_jar" ] || die "Recipe jar was not built"

  # Gradle needs memory to hold the whole repository's syntax trees
  heap="${YOKO_FORMAT_HEAP:--Xmx8g}"
  report="$target/build/reports/rewrite/rewrite.patch"
  rm -f "$report"

  cd "$target"
  # (a single-use daemon: a long-lived one can keep serving the classes of an earlier build of the recipes)
  ./gradlew --no-daemon --console=plain -q --init-script "$tool_dir/scripts/format/init.gradle" \
    -Dorg.gradle.jvmargs="$heap" \
    -Pyoko.format.recipeJar="$recipe_jar" \
    -Pyoko.format.config="$tool_dir/config/rewrite.yml" \
    -Pyoko.format.recipe="$recipe" \
    "$mode" > "$tmp.log" 2>&1 || {
      cat "$tmp.log" >&2
      rm -f "$tmp.log"
      die "Formatting failed"
    }
  rm -f "$tmp.log"

  if [ "$mode" = rewriteDryRun ] && [ -f "$report" ]; then
    echo "⚠ These files need formatting (run scripts/yoko-format.sh to fix):" >&2
    sed -n 's|^diff --git a/.* b/||p' "$report" >&2
    exit 1
  fi
  echo "✓ Formatted ($recipe)"
)
