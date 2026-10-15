import http from 'k6/http';
import { check } from 'k6';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';
// Requires >= 1,000 pre-funded distinct account IDs separated by comma; avoid single-row lock bottlenecks.
const accounts = (__ENV.SOURCE_ACCOUNT_IDS || '').split(',').map(s => s.trim()).filter(Boolean);
const target = Number(__ENV.TARGET_TPS || 1000);
export const options = {
  scenarios: { sustained: { executor: 'constant-arrival-rate', rate: target, timeUnit: '1s', duration: __ENV.DURATION || '2m', preAllocatedVUs: Number(__ENV.PRE_VUS || 300), maxVUs: Number(__ENV.MAX_VUS || 1500) } },
  thresholds: { http_req_failed: ['rate<0.01'], http_req_duration: ['p(95)<500'], dropped_iterations: ['count==0'] }
};
const base = __ENV.BASE_URL || 'http://localhost:8080';
export default function () {
  if (!accounts.length) throw new Error('SOURCE_ACCOUNT_IDS must contain disposable funded accounts');
  const account = accounts[Math.floor(Math.random() * accounts.length)];
  const res = http.post(`${base}/api/v1/bank-transfers`, JSON.stringify({ sourceAccountId: account, bankCode: 'DEMO', destinationAccount: '123456', amount: 1, currency: 'KRW' }), { headers: { 'Content-Type': 'application/json', 'Idempotency-Key': uuidv4() }, timeout: '5s' });
  check(res, { 'accepted 202': r => r.status === 202 });
}
