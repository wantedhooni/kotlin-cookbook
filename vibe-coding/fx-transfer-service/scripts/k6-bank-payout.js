import http from 'k6/http';
import { check, sleep } from 'k6';
export const options = { scenarios: { load: { executor: 'ramping-vus', startVUs: 1, stages: [
  { duration: '20s', target: 5 }, { duration: '40s', target: 20 }, { duration: '20s', target: 0 }
] } }, thresholds: { http_req_failed: ['rate<0.05'], http_req_duration: ['p(95)<1000'] } };
const base = __ENV.BASE_URL || 'http://localhost:8080';
const accountId = __ENV.SOURCE_ACCOUNT_ID;
export default function () {
 if (!accountId) throw new Error('Set SOURCE_ACCOUNT_ID to a funded demo account');
 const payload = JSON.stringify({sourceAccountId:accountId,bankCode:'DEMO',destinationAccount:'123456',amount:1,currency:'KRW'});
 const key = `${__VU}-${__ITER}-${Date.now()}`;
 const res = http.post(`${base}/api/v1/bank-transfers`,payload,{headers:{'Content-Type':'application/json','Idempotency-Key':key}});
 check(res,{'accepted': r => r.status === 202}); sleep(0.2);
}
