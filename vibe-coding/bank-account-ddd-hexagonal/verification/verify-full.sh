#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
command -v java >/dev/null || { echo 'JDK unavailable' >&2; exit 1; }
command -v docker >/dev/null || { echo 'Docker unavailable' >&2; exit 1; }
docker info >/dev/null || { echo 'Docker daemon unavailable' >&2; exit 1; }
if [[ -x ./gradlew ]]; then G=./gradlew; else G=gradle; command -v "$G" >/dev/null || { echo 'Gradle unavailable; install Gradle or add Wrapper' >&2; exit 1; }; fi
"$G" --no-daemon clean test
"$G" --no-daemon :api:bootJar
