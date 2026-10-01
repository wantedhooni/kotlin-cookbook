import http from 'k6/http';
import { check, sleep } from 'k6';
export const options={scenarios:{open_burst:{executor:'ramping-arrival-rate',startRate:50,timeUnit:'1s',preAllocatedVUs:100,maxVUs:1000,stages:[{target:200,duration:'30s'},{target:1000,duration:'1m'},{target:200,duration:'30s'}]}},thresholds:{http_req_failed:['rate<0.01'],http_req_duration:['p(95)<300','p(99)<800']}};
const BASE=__ENV.BASE_URL||'http://localhost:8080';
export default function(){const n=Math.floor(Math.random()*900000)+100000;const payload=JSON.stringify({accountId:`LOAD-${__VU % 100}`,symbol:'005930',side:'BUY',orderType:'LIMIT',quantity:1,price:70000});const r=http.post(`${BASE}/api/v1/orders`,payload,{headers:{'Content-Type':'application/json'}});check(r,{'201':x=>x.status===201});sleep(0.01);}
