# common-event

서비스 간 Kafka 메시지 계약을 공유하는 Kotlin 라이브러리입니다. 독립 실행 서비스가 아니며 `common-domain`에 의존합니다.

## 구현 내용

- `Topics`: 주문, KRX 요청·체결, 체결 확정 및 취소·정정 결과의 토픽 이름
- `TradingEvents`: 주문 접수·취소·정정, KRX 주문·체결, 확정 체결 이벤트 DTO

메시지 DTO는 `eventId`와 관련 주문/체결 식별자, 이벤트 시각 및 거래 데이터를 담습니다. 이벤트 발행·소비 처리는 각 서비스가 담당합니다.

## 빌드 및 테스트

저장소 루트에서 실행합니다.

```bash
cd securities/stock-trading-system
./gradlew :libs:common-event:build
./gradlew :libs:common-event:test
```

Kotlin/JVM 21을 사용하며, `:libs:common-domain`을 컴파일 의존성으로 사용합니다. 이 모듈에는 별도 테스트 소스가 없습니다.
