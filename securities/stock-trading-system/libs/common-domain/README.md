# common-domain

주문·계좌·잔고·결제에서 공유하는 Kotlin 도메인 모델과 정책 라이브러리입니다. 독립 실행 서비스가 아니며 `common-event`와 각 서비스에서 사용합니다.

## 구현 내용

- `Order`: 주문 검증 및 접수, 체결, 취소, 정정 상태 전이
- `TradingAccount`, `MarginPolicy`: 현금·신용 예약과 결제 보류, 매수 예약 한도 계산
- `Position`: 평균 매입가와 실현 손익 계산
- `Settlement`: 영업일 달력, 결제 의무 및 상태 모델
- `TradingPolicies`: 가격 호가 단위와 시장 세션별 주문 허용 정책. 호가 단위는 교육용 모의 정책입니다.

## 빌드 및 테스트

저장소 루트에서 아래 명령을 실행합니다. Gradle 설정과 wrapper는 `securities/stock-trading-system`에 있습니다.

```bash
cd securities/stock-trading-system
./gradlew :libs:common-domain:test
./gradlew :libs:common-domain:build
```

Kotlin/JVM 21을 사용합니다. 테스트는 Kotlin Test, JUnit 5, Kotest assertions를 사용합니다.
