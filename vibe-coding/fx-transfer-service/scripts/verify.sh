#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
command -v java >/dev/null || { echo 'Java 21 required'; exit 1; }
java -version
if [[ -x ./gradlew ]]; then GRADLE=./gradlew; elif command -v gradle >/dev/null; then GRADLE=gradle; else echo 'Gradle >=8.14 not installed; cannot verify compilation'; exit 2; fi
"$GRADLE" --no-daemon clean test assemble
if [[ "${RUN_CONTAINER_TESTS:-false}" == true ]]; then
  command -v docker >/dev/null || { echo 'Docker is required for Testcontainers'; exit 3; }
  "$GRADLE" --no-daemon :api:test
fi
