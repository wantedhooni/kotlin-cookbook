import http from 'k6/http';
import { check } from 'k6';
export const options = { vus: 20, iterations: 100, thresholds: { http_req_failed: ['rate<0.01'] } };
export default function () {
 const key = __ENV.IDEMPOTENCY_KEY;
 if (!key || !__ENV.SOURCE_ACCOUNT_ID) throw new Error('Set IDEMPOTENCY_KEY and SOURCE_ACCOUNT_ID');
 const payload = JSON.stringify({sourceAccountId: __ENV.SOURCE_ACCOUNT_ID,bankCode:'DEMO',destinationAccount:'123456',amount:1,currency:'KRW'});
 const r = http.post(`${__ENV.BASE_URL || 'http://localhost:8080'}/api/v1/bank-transfers`,payload,{headers:{'Content-Type':'application/json','Idempotency-Key':key}});
 check(r, {'accepted':x=>x.status===202});
}
