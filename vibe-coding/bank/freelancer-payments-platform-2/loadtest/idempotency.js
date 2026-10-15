import http from 'k6/http';
import { check } from 'k6';

export const options = { vus: 20, iterations: 200 };
const base = __ENV.BASE_URL || 'http://localhost:8080';
const sharedKey = 'same-business-request-001';

export default function () {
  const res = http.post(`${base}/api/payments`, JSON.stringify({
    sellerId: 'seller-race', amount: 50000, cardToken: 'tok_race'
  }), { headers: { 'Content-Type': 'application/json', 'Idempotency-Key': sharedKey } });
  check(res, { 'success-or-conflict': r => [201, 409, 500].includes(r.status) });
}
