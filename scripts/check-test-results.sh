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
    echo "usage:\t$0"
    echo "\tPrints failing tests and returns the number of failing tests; succeeds only if no tests fail"
  }

  # Process args
  [ $# -lt 1 ] || { usage; exit 1; }

  # Process the available results files
  grep "<testcase.*[^/]>" */build/test-results/test/TEST-*.xml 2>/dev/null \
  | sed -E 's#(.*)classname="([^"]*)"(.*)#\1\3@@\2#;s#.*name="([^"]*)".*@@(.*)#\2.\1#' \
  | {
        NUM_FAILURES=0
        while read -r line
        do
          # print the failing test
          echo "$line"
          # increment the failure count
          NUM_FAILURES=$((NUM_FAILURES + 1))
        done
        # return the number of failing tests
        exit $NUM_FAILURES
  }
)
