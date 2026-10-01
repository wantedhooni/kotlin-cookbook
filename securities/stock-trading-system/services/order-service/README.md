# order-service

주문 생성·조회와 취소·정정 요청 및 주문 상태를 관리하는 Spring Boot API 서비스입니다. `common-domain`과 `common-event`를 사용합니다.

## 구현 내용

- 지정가/시장가 주문 입력과 가격 호가 단위 검증 후 주문 저장 및 outbox 기록
- 매수 시 계좌 자금 예약, 매도 시 보유 수량 예약을 각각 HTTP 연동
- Kafka 주문 승인·거절·체결·취소·정정 이벤트 반영 및 이벤트 중복 처리
- 주문 조회, 취소 요청, 가격 정정 요청과 정정 이력 조회 API
- PostgreSQL/JPA, Flyway, Actuator/Prometheus 및 OTLP tracing 의존성

주요 API는 `/api/v1/orders` 및 `/api/v1/orders/{orderId}` 하위 경로입니다.

## 실행 및 테스트

저장소 루트에서 실행합니다. PostgreSQL, Kafka 및 account/balance 서비스가 필요합니다. 로컬 인프라는 `docker compose up -d`로 실행할 수 있습니다.

```bash
cd securities/stock-trading-system
./gradlew :services:order-service:test
./gradlew :services:order-service:bootRun
```

기본 포트는 `8080`입니다. DB 연결은 `DB_URL`/`DB_USER`/`DB_PASSWORD`(기본 `jdbc:postgresql://localhost:5432/trading`, `trading`/`trading`), Kafka는 `KAFKA_BOOTSTRAP`(기본 `localhost:9092`)으로 설정합니다. 연동 주소는 `BALANCE_SERVICE_URL`(기본 `http://localhost:8083`), `ACCOUNT_SERVICE_URL`(기본 `http://localhost:8085`)입니다.
