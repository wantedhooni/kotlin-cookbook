import http from 'k6/http';
import { check } from 'k6';

export const options = {
  scenarios: {
    payment: {
      executor: 'constant-arrival-rate',
      rate: 100,
      timeUnit: '1s',
      duration: '60s',
      preAllocatedVUs: 30,
      maxVUs: 200,
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<300', 'p(99)<700'],
  },
};

const base = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  const key = `k6-${__VU}-${__ITER}-${Date.now()}`;
  const res = http.post(`${base}/api/payments`, JSON.stringify({
    sellerId: `seller-${__VU % 20}`,
    amount: 10000 + (__ITER % 1000),
    cardToken: 'tok_k6',
  }), { headers: { 'Content-Type': 'application/json', 'Idempotency-Key': key } });
  check(res, { 'created': r => r.status === 201 });
}
