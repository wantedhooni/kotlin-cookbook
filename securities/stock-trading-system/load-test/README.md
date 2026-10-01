# k6 부하 테스트

1. 부하 계좌를 Account Service에 미리 seed 합니다.
2. `k6 run load-test/k6/order-burst.js`로 장 시작 Burst를 재현합니다.
3. `k6 run load-test/k6/mixed-trading.js`로 80% 주문 / 20% 조회를 재현합니다.

기본 SLO: HTTP 실패율 < 1%, 주문 API p95 < 300ms, p99 < 800ms. Kafka consumer lag과 DB lock wait가 함께 증가하면 애플리케이션 Pod만 늘리지 말고 Kafka partition/DB 병목을 먼저 확인합니다.
