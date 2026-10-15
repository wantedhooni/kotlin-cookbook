#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
D="$(mktemp -d)"
trap 'rm -rf "$D"' EXIT
kotlinc bank-context/src/main/kotlin account-context/src/main/kotlin transfer-context/src/main/kotlin verification/SagaSmoke.kt -include-runtime -d "$D/smoke.jar"
java -jar "$D/smoke.jar"
python3 - <<'PY'
import json, pathlib, zipfile
p = pathlib.Path('keycloak/bank-realm.json')
json.loads(p.read_text())
import yaml
yaml.safe_load(pathlib.Path('compose.yaml').read_text())
yaml.safe_load(pathlib.Path('api/src/main/resources/application.yml').read_text())
print('PASS: Keycloak JSON and YAML syntax')
PY
