# mock-krx

Kafka 기반으로 KRX 체결 흐름을 시뮬레이션하는 Spring Boot 서비스입니다. 실제 거래소 연동이나 실거래 가격 산정 서비스는 아닙니다.

## 구현 내용

- 모의 주문 요청을 중복 수신 방지 후 첫 체결과 잔여 체결로 나눠 처리
- 잔여 주문은 2초 후 체결하며, 미체결 주문 취소·가격 정정 및 먼저 도착한 요청을 처리
- 지정가는 주문 가격, 시장가는 설정된 모의 시장 가격으로 체결가를 생성
- 취소·정정·체결 결과를 공통 Kafka 이벤트로 발행

## 실행 및 테스트

저장소 루트에서 실행합니다. Kafka가 필요하며, 로컬 의존 서비스를 띄우려면 `docker compose up -d`를 실행합니다.

```bash
cd securities/stock-trading-system
./gradlew :services:mock-krx:test
./gradlew :services:mock-krx:bootRun
```

기본 포트는 `8084`, Kafka 주소는 `KAFKA_BOOTSTRAP`(기본 `localhost:9092`), 시장 기준가는 `MOCK_MARKET_PRICE`(기본 `70000`)로 설정합니다. 단위 테스트는 모의 매칭 엔진을 검증합니다.
