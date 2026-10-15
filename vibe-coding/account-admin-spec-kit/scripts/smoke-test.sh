#!/usr/bin/env bash
set -euo pipefail
BASE_URL="${BASE_URL:-http://localhost:8080}"
: "${ADMIN_USERNAME:=admin}"
: "${ADMIN_PASSWORD:?Set ADMIN_PASSWORD before running the smoke test}"
SUFFIX="$(date +%s)$$"
LOGIN="smoke_${SUFFIX}"
BODY="$(printf '{"loginId":"%s","email":"%s@example.test","displayName":"Smoke Test","role":"USER"}' "$LOGIN" "$LOGIN")"
TMP="$(mktemp)"; trap 'rm -f "$TMP"' EXIT
printf '1. create... '
STATUS=$(curl -sS -o "$TMP" -w '%{http_code}' -u "$ADMIN_USERNAME:$ADMIN_PASSWORD" -H 'Content-Type: application/json' -d "$BODY" "$BASE_URL/api/admin/accounts")
[[ "$STATUS" == 201 ]] || { cat "$TMP"; exit 1; }
ACCOUNT_ID=$(python3 -c 'import json,sys;print(json.load(open(sys.argv[1]))["id"])' "$TMP")
echo "$ACCOUNT_ID"
printf '2. read... '
curl -fsS -u "$ADMIN_USERNAME:$ADMIN_PASSWORD" "$BASE_URL/api/admin/accounts/$ACCOUNT_ID" >/dev/null && echo OK
printf '3. suspend... '
curl -fsS -u "$ADMIN_USERNAME:$ADMIN_PASSWORD" -X PATCH -H 'Content-Type: application/json' -d '{"status":"SUSPENDED"}' "$BASE_URL/api/admin/accounts/$ACCOUNT_ID/status" >/dev/null && echo OK
printf '4. delete... '
curl -fsS -u "$ADMIN_USERNAME:$ADMIN_PASSWORD" -X DELETE "$BASE_URL/api/admin/accounts/$ACCOUNT_ID" >/dev/null && echo OK
printf '5. verify gone... '
STATUS=$(curl -sS -o /dev/null -w '%{http_code}' -u "$ADMIN_USERNAME:$ADMIN_PASSWORD" "$BASE_URL/api/admin/accounts/$ACCOUNT_ID")
[[ "$STATUS" == 404 ]] && echo OK || { echo "Unexpected HTTP $STATUS"; exit 1; }
