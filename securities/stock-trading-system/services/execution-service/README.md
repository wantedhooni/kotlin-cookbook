# execution-service

모의 거래소에서 전달된 체결을 저장하고 공통 체결 이벤트로 발행하는 Spring Boot 서비스입니다. `common-domain` 및 `common-event`에 의존합니다.

## 구현 내용

- `trading.krx.executions` Kafka 이벤트를 체결 ID 기준으로 중복 확인 후 PostgreSQL에 기록
- 저장 트랜잭션에 outbox 이벤트를 함께 기록하고, 주기적으로 Kafka에 발행
- 주문 ID별 체결 내역 조회 API: `GET /api/v1/executions/orders/{orderId}`
- JPA/Flyway, Actuator/Prometheus, OTLP tracing 지원

## 실행 및 테스트

저장소 루트에서 실행합니다. 로컬 의존 서비스를 띄우려면 먼저 `docker compose up -d`를 실행합니다(PostgreSQL, Kafka).

```bash
cd securities/stock-trading-system
./gradlew :services:execution-service:test
./gradlew :services:execution-service:bootRun
```

기본 포트는 `8082`입니다. `DB_URL`/`DB_USER`/`DB_PASSWORD`(기본 `jdbc:postgresql://localhost:5432/trading`, `trading`/`trading`), `KAFKA_BOOTSTRAP`(기본 `localhost:9092`), `SERVER_PORT`를 사용합니다. outbox 발행 간격은 `outbox.publish-delay-ms`로 설정하며 기본값은 300ms입니다.
