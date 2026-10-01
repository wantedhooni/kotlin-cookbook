# PG Settlement Batch Prototype

Spring Batch와 JPA로 PG 결제 데이터를 읽어 정산 결과를 생성하는 초기 예제입니다. Job Parameter의 정산일에 승인된 결제를 페이지 단위로 읽고, 주문 존재 여부와 주문 금액을 대사한 뒤 결제액에서 PG 수수료를 뺀 결과를 저장합니다.

## 처리 흐름

```text
PgPaymentEntity (정산일 승인 건, 페이지 크기 1,000)
    → 주문 존재/결제 금액 대사
    → PgSettlementEntity
```

정산 결과 상태:

- `MATCHED`: 주문이 있고 주문 금액과 PG 결제 금액이 일치
- `ORDER_NOT_FOUND`: 연결된 주문이 없음
- `AMOUNT_MISMATCH`: 주문 금액과 결제 금액이 다름

현재 모델은 승인 거래 중심이며 부분 취소, 재실행 멱등성, 운영 스키마 migration은 완성되어 있지 않습니다. 소스에 Spring Boot 애플리케이션 진입점도 확인되지 않으므로 현재 상태 그대로는 실행 가능한 Batch 앱이 아닐 수 있습니다.

## 기술 스택

- Kotlin 2.3.21, Java 21
- Spring Boot 4.1.1, Spring Batch 6 계열, Spring Data JPA
- PostgreSQL

## 데이터베이스 준비

Compose 파일은 PostgreSQL과 Redis를 올리지만 애플리케이션 서비스는 포함되어 있지 않습니다. 기본 PostgreSQL 값은 DB/사용자 `pension`, 비밀번호 `pension-local`, 포트 `5432`입니다.

```bash
docker compose up -d postgres
```

실행 가능한 앱 구성을 추가한 경우 datasource를 환경변수로 설정할 수 있습니다.

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/pension \
SPRING_DATASOURCE_USERNAME=pension \
SPRING_DATASOURCE_PASSWORD=pension-local \
./gradlew bootRun --args='--spring.batch.job.enabled=true settlementDate=2026-09-13'
```

Gradle 검증:

```bash
./gradlew test
```

이 저장소의 `pg-sattlement-1` 디렉터리는 이름을 유지한 실험용 프로젝트입니다. 부분 취소를 포함한 별도 정산 예제는 [`../pg-settlement-batch`](../pg-settlement-batch/)를 참고하세요.
