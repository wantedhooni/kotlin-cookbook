# 성능 및 확장 기준

## 목표 SLO

| 지표 | 기본 목표 | 확장/조사 기준 |
|---|---:|---:|
| 주문 API p95 | < 300ms | 5분 이상 300ms 초과 |
| 주문 API p99 | < 800ms | 5분 이상 800ms 초과 |
| HTTP 5xx | < 1% | 1% 이상 |
| Order Service CPU | < 65% | HPA scale-out |
| Order Service Memory | < 75% | HPA scale-out |
| Execution Service CPU | < 60% | HPA scale-out |
| Kafka consumer lag | 정상 시 지속 감소 | 증가 추세가 5분 이상 지속되면 partition/consumer 점검 |
| DB connection pool | < 80% | 80% 이상 지속 시 쿼리/풀/DB 병목 조사 |
| DB lock wait | 낮고 일시적 | 계좌/잔고 hot-key 집중 시 샤딩/직렬화 전략 검토 |

## 확장 순서

1. API CPU/Memory가 먼저 포화되면 Pod HPA를 적용한다.
2. Kafka lag가 증가하지만 CPU가 낮으면 consumer 병렬도와 partition 수를 먼저 확인한다.
3. DB lock wait가 증가하면 Pod만 늘리지 않는다. 동일 계좌 hot-key, 인덱스, 트랜잭션 범위를 확인한다.
4. 조회 부하가 원인이면 Redis hit ratio와 TTL을 확인하고 Read Model 분리를 검토한다.
5. 스케일 다운은 최소 300초 안정화 후 수행해 장중 flap을 방지한다.

## k6 실행

```bash
./scripts/seed-load-accounts.sh
k6 run load-test/k6/order-burst.js
k6 run load-test/k6/mixed-trading.js
```

운영 수준에서는 Kafka consumer lag를 Prometheus Adapter 또는 별도 autoscaler의 External Metric으로 노출하여 CPU와 함께 스케일 조건으로 사용한다.
