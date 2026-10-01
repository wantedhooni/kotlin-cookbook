# order-router

주문 서비스가 발행한 접수·취소·정정 이벤트를 시장 세션 정책에 따라 KRX용 Kafka 요청으로 전달하는 Spring Boot 서비스입니다. `common-domain`과 `common-event`를 사용합니다.

## 구현 내용

- 주문 접수 시 현재 세션에서 주문 유형을 검사하고, 거절 또는 접수 승인 후 KRX 주문 요청 발행
- 정정 요청의 세션 허용 여부를 검사하고, 취소·정정 요청을 해당 KRX 토픽으로 전달
- `MARKET_SESSION_OVERRIDE`가 `AUTO`이면 Asia/Seoul 시스템 시각 기준으로 장중 세션을 판별하고, 그 외에는 지정 세션을 사용

기본 세션은 `REGULAR`로, 시간과 관계없이 로컬 데모 주문이 진행됩니다. `AUTO` 설정 시 주말·세션 시간 외에는 `CLOSED`입니다.

## 실행 및 테스트

저장소 루트에서 실행합니다. Kafka가 필요하며, 로컬 의존 서비스를 띄우려면 `docker compose up -d`를 실행합니다.

```bash
cd securities/stock-trading-system
./gradlew :services:order-router:test
./gradlew :services:order-router:bootRun
```

기본 포트는 `8081`, Kafka 주소는 `KAFKA_BOOTSTRAP`(기본 `localhost:9092`), 세션 설정은 `MARKET_SESSION_OVERRIDE`(기본 `REGULAR`)입니다. 테스트는 시장 세션 판별 정책을 검증합니다.
