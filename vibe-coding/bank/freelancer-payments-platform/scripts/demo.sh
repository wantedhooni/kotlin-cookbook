#!/usr/bin/env bash
set -euo pipefail
BASE=${BASE_URL:-http://localhost:8080}

curl -sS -X POST "$BASE/api/payments"   -H 'Content-Type: application/json'   -H 'Idempotency-Key: demo-pay-001'   -d '{"sellerId":"seller-1","amount":100000,"cardToken":"tok_demo"}' | jq .

curl -sS -X POST "$BASE/api/settlements/run" | jq .
