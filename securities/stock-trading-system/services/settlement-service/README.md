# settlement-service

체결 이벤트를 영업일 기준 D+2 결제 의무로 등록하고 만기 결제를 계좌 서비스에 요청하는 Spring Boot 서비스입니다. `common-domain`과 `common-event`를 사용합니다.

## 구현 내용

- `EXECUTION_BOOKED` 이벤트를 받아 체결일(Asia/Seoul)과 영업일 달력으로 결제일 계산; 같은 체결 ID는 중복 등록하지 않음
- 평일 기본 16:05(Asia/Seoul)에 만기 건을 처리하고 성공·연체 결과를 저장
- 날짜별 내부 일괄 처리 API 및 계좌별 예정 지급·수취·연체 요약 API
- PostgreSQL/JPA와 Flyway, Kafka 소비, 계좌 서비스 HTTP 연동

## 실행 및 테스트

저장소 루트에서 실행합니다. PostgreSQL, Kafka, account-service가 필요합니다. 로컬 인프라는 `docker compose up -d`로 실행할 수 있습니다.

```bash
cd securities/stock-trading-system
./gradlew :services:settlement-service:test
./gradlew :services:settlement-service:bootRun
```

기본 포트는 `8086`입니다. DB와 Kafka 연결은 `DB_URL`/`DB_USER`/`DB_PASSWORD` 및 `KAFKA_BOOTSTRAP`(기본 `localhost:9092`), 계좌 주소는 `ACCOUNT_SERVICE_URL`(기본 `http://localhost:8085`)로 설정합니다. 달력 휴일은 `SETTLEMENT_HOLIDAYS`(쉼표 구분 날짜), 스케줄은 `SETTLEMENT_CRON`, 처리일 재정의는 `SETTLEMENT_DATE_OVERRIDE`로 설정할 수 있습니다.
