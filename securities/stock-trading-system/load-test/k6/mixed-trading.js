import http from 'k6/http';
import { check } from 'k6';
export const options={vus:50,duration:'2m',thresholds:{http_req_failed:['rate<0.01'],http_req_duration:['p(95)<250']}};
const ORDER=__ENV.ORDER_URL||'http://localhost:8080'; const ACCOUNT=__ENV.ACCOUNT_URL||'http://localhost:8085';
export default function(){if(Math.random()<0.8){const r=http.post(`${ORDER}/api/v1/orders`,JSON.stringify({accountId:`LOAD-${__VU%100}`,symbol:'005930',side:'BUY',orderType:'LIMIT',quantity:1,price:70000}),{headers:{'Content-Type':'application/json'}});check(r,{'order accepted':x=>x.status===201});}else{const r=http.get(`${ACCOUNT}/api/v1/accounts/LOAD-${__VU%100}`);check(r,{'account readable':x=>x.status===200});}}
