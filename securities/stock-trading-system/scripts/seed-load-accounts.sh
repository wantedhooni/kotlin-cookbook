#!/usr/bin/env bash
set -euo pipefail
ACCOUNT_URL="${ACCOUNT_URL:-http://localhost:8085}"
for i in $(seq 0 99); do
  curl -fsS -X POST "$ACCOUNT_URL/internal/v1/accounts/seed" -H 'Content-Type: application/json' \
    -d "{\"accountId\":\"LOAD-$i\",\"cashBalance\":1000000000,\"creditLimit\":0}" >/dev/null
done
echo "Seeded LOAD-0 .. LOAD-99"
