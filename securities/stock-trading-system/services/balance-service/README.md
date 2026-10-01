# balance-service

계좌별 종목 보유 수량·평균가·실현 손익과 매도 가능 수량을 관리하는 Spring Boot 서비스입니다. `common-domain`과 `common-event`를 사용합니다.

## 구현 내용

- 보유 포지션 조회·목록 및 초기 포지션 설정 API
- 매도 주문 수량 예약·해제와 체결·취소 이벤트에 따른 포지션 갱신
- 체결 이벤트 중복 처리를 막고, Redis에 조회 결과를 30초간 best-effort 캐시
- PostgreSQL/JPA 잠금·Flyway 마이그레이션, Kafka 이벤트 소비, Actuator/Prometheus 및 OTLP tracing 의존성

주요 API: `/api/v1/balances/{accountId}/{symbol}`, `/api/v1/balances/{accountId}`, `/internal/v1/positions/seed`, `/internal/v1/sell-reservations`.

## 실행 및 테스트

저장소 루트에서 실행합니다. 로컬 의존 서비스를 띄우려면 먼저 `docker compose up -d`를 실행합니다(PostgreSQL, Redis, Kafka).

```bash
cd securities/stock-trading-system
./gradlew :services:balance-service:test
./gradlew :services:balance-service:bootRun
```

기본 포트는 `8083`입니다. `DB_URL`/`DB_USER`/`DB_PASSWORD`(기본 `jdbc:postgresql://localhost:5432/trading`, `trading`/`trading`), `REDIS_HOST`/`REDIS_PORT`(기본 `localhost:6379`), `KAFKA_BOOTSTRAP`(기본 `localhost:9092`), `SERVER_PORT`로 설정합니다.
