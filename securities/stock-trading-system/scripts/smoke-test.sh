#!/usr/bin/env bash
set -euo pipefail
ORDER_URL="${ORDER_URL:-http://localhost:8080}"
BALANCE_URL="${BALANCE_URL:-http://localhost:8083}"
EXECUTION_URL="${EXECUTION_URL:-http://localhost:8082}"
ACCOUNT_URL="${ACCOUNT_URL:-http://localhost:8085}"
SETTLEMENT_URL="${SETTLEMENT_URL:-http://localhost:8086}"
command -v jq >/dev/null || { echo "jq is required"; exit 1; }

echo "[0] Seed account"
curl -fsS -X POST "$ACCOUNT_URL/internal/v1/accounts/seed" -H 'Content-Type: application/json' -d '{"accountId":"ACC-SMOKE","cashBalance":2000000,"creditLimit":500000}' | jq .

echo "[1] Place BUY 10 @ 70,000"
response=$(curl -fsS -X POST "$ORDER_URL/api/v1/orders" -H 'Content-Type: application/json' -d '{"accountId":"ACC-SMOKE","symbol":"005930","side":"BUY","orderType":"LIMIT","quantity":10,"price":70000}')
order_id=$(echo "$response" | jq -r '.orderId')
echo "orderId=$order_id"

echo "[2] Request price correction to 69,000 (races with Mock KRX partial fill)"
curl -fsS -X POST "$ORDER_URL/api/v1/orders/$order_id/corrections" -H 'Content-Type: application/json' -d '{"newPrice":69000}' | jq .

echo "[3] Wait for partial/full fill"
sleep 4
curl -fsS "$ORDER_URL/api/v1/orders/$order_id" | jq .
echo "Correction history:"; curl -fsS "$ORDER_URL/api/v1/orders/$order_id/corrections" | jq .

echo "[4] Executions / position / account before D+2"
executions=$(curl -fsS "$EXECUTION_URL/api/v1/executions/orders/$order_id")
echo "$executions" | jq .
curl -fsS "$BALANCE_URL/api/v1/balances/ACC-SMOKE/005930" | jq .
curl -fsS "$ACCOUNT_URL/api/v1/accounts/ACC-SMOKE" | jq .

echo "[5] Settlement obligations"
summary=$(curl -fsS "$SETTLEMENT_URL/api/v1/settlements/accounts/ACC-SMOKE")
echo "$summary" | jq .
due_date=$(echo "$summary" | jq -r '[.obligations[].settlementDate] | max')
test "$due_date" != "null" || { echo "no settlement obligation found"; exit 1; }

echo "[6] Force D+2 batch for $due_date"
curl -fsS -X POST "$SETTLEMENT_URL/internal/v1/settlements/process?businessDate=$due_date" | jq .

echo "[7] Account after settlement"
curl -fsS "$ACCOUNT_URL/api/v1/accounts/ACC-SMOKE" | jq .
curl -fsS "$SETTLEMENT_URL/api/v1/settlements/accounts/ACC-SMOKE" | jq .
