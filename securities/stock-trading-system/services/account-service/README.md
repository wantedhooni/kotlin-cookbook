# account-service

계좌의 현금·신용 한도와 매수 자금 예약, 체결 후 결제 보류 및 정산 처리를 담당하는 Spring Boot 서비스입니다. `common-domain`과 `common-event`를 사용합니다.

## 구현 내용

- 계좌 조회 및 초기 잔고 설정, 매수 예약·해제 API
- 시장가 기준가·버퍼와 기본 증거금률을 이용한 매수 예약; 지정가 매수는 주문 가격을 보호 가격으로 사용
- 체결·취소·거절 이벤트에 따른 예약 반영 및 멱등 처리
- 매수 결제 부족액·연체 처리와 매도 대금 수취
- PostgreSQL/JPA 비관적 잠금, Flyway 스키마 마이그레이션, Kafka 이벤트 소비

주요 API: `GET /api/v1/accounts/{accountId}`, `/internal/v1/accounts/seed`, `/internal/v1/buy-reservations`, `/internal/v1/accounts/settlements/{executionId}`.

## 실행 및 테스트

저장소 루트에서 실행합니다. 로컬 의존 서비스를 띄우려면 먼저 `docker compose up -d`를 실행합니다(PostgreSQL, Kafka).

```bash
cd securities/stock-trading-system
./gradlew :services:account-service:test
./gradlew :services:account-service:bootRun
```

기본 포트는 `8085`입니다. `DB_URL`/`DB_USER`/`DB_PASSWORD`(기본 `jdbc:postgresql://localhost:5432/trading`, `trading`/`trading`), `KAFKA_BOOTSTRAP`(기본 `localhost:9092`), `SERVER_PORT`로 연결을 설정합니다. 마진 설정은 `DEFAULT_MARGIN_RATE`(기본 `1.00`), `MARKET_BUFFER_RATE`(`0.10`), `MARKET_REFERENCE_PRICE`(`70000`)입니다. 통합 테스트는 Spring Boot Test와 Testcontainers를 사용합니다.
