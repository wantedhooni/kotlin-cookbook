#!/usr/bin/env sh
set -eu
if command -v gradle >/dev/null 2>&1; then
  exec gradle bootRun "$@"
fi
echo "Gradle is not installed. Use Gradle 8.14.x+ or run: docker build -t dbs-deposit-api ." >&2
exit 1
