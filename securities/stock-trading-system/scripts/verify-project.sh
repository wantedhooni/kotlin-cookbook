#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"; cd "$ROOT"
echo "[1/6] Required business files"
required=(
  libs/common-domain/src/main/kotlin/com/example/trading/domain/Order.kt
  libs/common-domain/src/main/kotlin/com/example/trading/domain/TradingAccount.kt
  libs/common-domain/src/main/kotlin/com/example/trading/domain/Settlement.kt
  services/order-service/src/main/kotlin/com/example/trading/orderservice/application/OrderApplicationService.kt
  services/mock-krx/src/main/kotlin/com/example/trading/mockkrx/adapter/in/kafka/MockKrxListener.kt
  services/account-service/src/main/kotlin/com/example/trading/accountservice/application/AccountApplicationService.kt
  services/settlement-service/src/main/kotlin/com/example/trading/settlementservice/application/SettlementApplicationService.kt
  load-test/k6/order-burst.js
  infrastructure/kubernetes/autoscaling.yaml
  infrastructure/monitoring/grafana/dashboards/trading-overview.json
)
for f in "${required[@]}"; do test -s "$f" || { echo "missing: $f"; exit 1; }; done

echo "[2/6] Domain/event compilation"
if command -v kotlinc >/dev/null 2>&1; then
  rm -rf /tmp/trading-domain /tmp/trading-events; mkdir -p /tmp/trading-domain /tmp/trading-events
  kotlinc libs/common-domain/src/main/kotlin/com/example/trading/domain/*.kt -d /tmp/trading-domain
  kotlinc -classpath /tmp/trading-domain libs/common-event/src/main/kotlin/com/example/trading/event/*.kt -d /tmp/trading-events
else echo "kotlinc unavailable: skipped"; fi

echo "[3/6] Flyway migrations"
test -s services/order-service/src/main/resources/db/migration/V2__order_correction.sql
test -s services/account-service/src/main/resources/db/migration/V2__settlement_accounting.sql
test -s services/settlement-service/src/main/resources/db/migration/V1__settlement_schema.sql

echo "[4/6] Testcontainers/DLT test sources"
grep -q "PostgreSQLContainer" services/account-service/src/test/kotlin/com/example/trading/accountservice/AccountConcurrencyIntegrationTest.kt
grep -q "KafkaContainer" services/execution-service/src/test/kotlin/com/example/trading/executionservice/integration/ExecutionKafkaIntegrationTest.kt
grep -q "DLT" services/execution-service/src/test/kotlin/com/example/trading/executionservice/integration/ExecutionKafkaIntegrationTest.kt

echo "[5/6] YAML / JSON"
python - <<'PY2'
import json, pathlib
json.load(open('infrastructure/monitoring/grafana/dashboards/trading-overview.json'))
try:
 import yaml
 for p in pathlib.Path('.').rglob('*.yml'):
  list(yaml.safe_load_all(p.read_text()))
 for p in pathlib.Path('infrastructure/kubernetes').glob('*.yaml'):
  list(yaml.safe_load_all(p.read_text()))
 print('YAML/JSON parse OK')
except ModuleNotFoundError:
 print('PyYAML unavailable; JSON validated, YAML parse skipped')
PY2

echo "[6/6] Gradle tests"
if [ "${RUN_GRADLE_TESTS:-0}" = "1" ]; then ./gradlew test; else echo "Skipped. RUN_GRADLE_TESTS=1 ./scripts/verify-project.sh"; fi
echo "Project verification completed."
