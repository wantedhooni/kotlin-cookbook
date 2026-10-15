#!/usr/bin/env bash
# Local-machine end-to-end gate: requires Java 21, Gradle 8.14+, Python 3, Docker Compose v2.
set -Eeuo pipefail
cd "$(dirname "$0")/.."
command -v java >/dev/null || { echo 'JDK missing' >&2; exit 2; }
command -v docker >/dev/null || { echo 'Docker missing' >&2; exit 2; }
command -v python3 >/dev/null || { echo 'Python 3 missing' >&2; exit 2; }
docker info >/dev/null || { echo 'Docker daemon not accessible' >&2; exit 2; }
docker compose version >/dev/null || { echo 'Docker Compose v2 missing' >&2; exit 2; }
if [[ -x ./gradlew ]]; then GRADLE=./gradlew; else GRADLE=gradle; command -v gradle >/dev/null || { echo 'Gradle missing' >&2; exit 2; }; fi
LOG_DIR="${E2E_LOG_DIR:-$(pwd)/verification/results}"
mkdir -p "$LOG_DIR"
APP_PID=''
cleanup() {
  if [[ -n "$APP_PID" ]]; then kill "$APP_PID" 2>/dev/null || true; wait "$APP_PID" 2>/dev/null || true; fi
  if [[ "${E2E_STOP_COMPOSE:-0}" == '1' ]]; then docker compose down; fi
}
trap cleanup EXIT
"$GRADLE" --no-daemon clean test :api:bootJar 2>&1 | tee "$LOG_DIR/gradle.log"
docker compose up -d --wait postgres kafka keycloak 2>&1 | tee "$LOG_DIR/compose.log"
"$GRADLE" --no-daemon :api:bootRun > "$LOG_DIR/spring-boot.log" 2>&1 &
APP_PID=$!
# Health readiness is bounded; process crash fails fast.
ready=0
for i in $(seq 1 90); do
  if ! kill -0 "$APP_PID" 2>/dev/null; then echo 'Spring Boot process stopped' >&2; tail -70 "$LOG_DIR/spring-boot.log" >&2; exit 1; fi
  if python3 - <<'PY'
from urllib.request import urlopen
try:
    with urlopen('http://localhost:8080/actuator/health', timeout=2) as result:
        assert result.status == 200
except Exception:
    raise SystemExit(1)
PY
  then ready=1; break; fi
  sleep 2
done
if [[ "$ready" != '1' ]]; then echo 'Spring Boot health timed out' >&2; tail -70 "$LOG_DIR/spring-boot.log" >&2; exit 1; fi
python3 verification/api_e2e.py 2>&1 | tee "$LOG_DIR/api-e2e.log"
echo 'ALL CHECKS PASSED: Gradle tests, bootJar, Spring Boot health, HTTP API E2E'
