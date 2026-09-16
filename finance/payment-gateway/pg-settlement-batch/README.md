# PG Settlement Batch Example

Kotlin + JPA + QueryDSL + Spring Batch로 구현한 **PG 정산 예제 프로젝트**입니다.

부분취소를 포함해 PG 승인/취소 이벤트를 원장으로 보존하고, 정산일 기준으로 QueryDSL에서 집계한 뒤 Spring Batch로 정산 결과를 생성합니다.

## 기술 스택

- Java 21
- Kotlin 2.4.20
- Spring Boot 4.0.8
- Spring Batch 6.x
- Spring Data JPA / Hibernate
- QueryDSL JPA 5.1.0 (Jakarta)
- PostgreSQL 17
- Flyway
- Gradle Kotlin DSL

> Spring Boot 4.0.8은 2026-08-20 공개된 안정 릴리스이며, Spring Batch 6.0.5는 2026-08-20 공개된 6.0.x 안정 릴리스입니다. 이 프로젝트는 Spring Batch 6의 패키지/API 형태를 사용합니다.

---

## 1. 정산 모델

부분취소가 있는 결제 시스템에서는 `PgPayment` 한 Row만 계속 덮어쓰기보다 **PG 이벤트를 별도 원장으로 보존**하는 방식이 재처리와 대사에 유리합니다.

```text
Order
  │
  └── PgPayment                 결제의 현재 누적 상태
          │
          └── PgTransaction     승인/부분취소/전체취소 이벤트 원장
                    │
                    ▼
              Settlement Batch
                    │
                    ▼
               PgSettlement     날짜별 정산 결과
```

### 역할

| 모델 | 역할 |
|---|---|
| `OrderEntity` | 서비스 주문 원장 |
| `PgPaymentEntity` | PG 결제 1건과 누적 승인/취소 상태 |
| `PgTransactionEntity` | 승인, 부분취소, 전체취소의 불변 이벤트 원장 |
| `PgSettlementEntity` | 정산일 + PG TID 단위 정산 결과 |

### 부분취소 예

9월 12일 승인:

```text
승인금액       +100,000
PG 수수료       +3,000
정산액          97,000
```

9월 13일 30,000원 부분취소, 수수료 900원 환급:

```text
승인              0
부분취소       -30,000
수수료 증감       -900

정산액
= 0 - 30,000 - (-900)
= -29,100
```

누적 실질 정산액은 `97,000 - 29,100 = 67,900`원이 됩니다.

이 프로젝트에서는 `fee_amount`의 부호를 다음처럼 정의합니다.

```text
PG에 지급하는 수수료      양수 (+)
취소로 반환되는 수수료    음수 (-)
```

따라서 날짜별 정산식은 항상 같습니다.

```text
정산액 = 승인금액 - 취소금액 - 수수료증감액
```

---

## 2. 왜 거래 이벤트를 별도 저장하는가

다음처럼 `PgPaymentEntity`의 상태만 바꾸면 과거 시점의 정산 근거가 사라집니다.

```text
100,000 승인
   ↓
30,000 부분취소
   ↓
현재 잔액 70,000
```

정산에서는 **언제 승인됐고, 언제 취소됐으며, 각 날짜에 수수료가 어떻게 증감했는지**가 필요합니다.

그래서 `pg_transaction`에는 다음처럼 남깁니다.

```text
2026-09-12 APPROVAL        100,000   fee +3,000
2026-09-13 PARTIAL_CANCEL   30,000   fee   -900
```

이 방식의 장점:

- 특정 정산일 재처리 가능
- PG 파일/API 재수집 후 재대사 가능
- 부분취소가 승인일과 다른 날짜에 발생해도 처리 가능
- 수수료 정책 변경 시 원장 기반 재계산 가능
- 장애 발생 시 정산 근거 추적 가능

---

## 3. Entity 구조

### `PgPaymentEntity`

결제의 현재 누적 상태입니다.

```kotlin
approvedAmount = 100000
canceledAmount = 30000
status = PARTIALLY_CANCELLED
```

현재 유효 결제금액:

```text
100,000 - 30,000 = 70,000
```

### `PgTransactionEntity`

승인/취소 이벤트 자체입니다.

```kotlin
enum class PgTransactionType {
    APPROVAL,
    PARTIAL_CANCEL,
    CANCEL,
}
```

`pg_event_id`에는 PG가 제공하는 고유 거래/이벤트 ID를 저장해 중복 수집을 방지합니다.

### `PgSettlementEntity`

정산 결과의 PK는 다음 멱등 키를 사용합니다.

```text
settlementDate|pgTid
```

예:

```text
2026-09-13|PG-TID-001
```

같은 정산일을 다시 실행해도 논리적으로 같은 정산 Row를 대상으로 하도록 하기 위한 구조입니다.

---

## 4. Batch 처리 흐름

```text
pg_transaction
      │
      ▼
QuerydslPagingItemReader
      │
      │ settlement_date + pg_tid GROUP BY
      ▼
PgSettlementSource
      │
      ▼
PgSettlementProcessor
      │
      ├── 주문 존재 여부 검사
      ├── 주문금액 ↔ PG 승인금액 대사
      ├── 누적 취소금액 검증
      └── 정산액 계산
      │
      ▼
JpaItemWriter
      │
      ▼
pg_settlement
```

Chunk size와 Reader page size는 예제에서 모두 `1,000`입니다.

---

## 5. QueryDSL + Spring Batch Reader

Spring Batch에는 QueryDSL 전용 ItemReader가 없으므로 `AbstractPagingItemReader`를 상속한 `QuerydslPagingItemReader`를 구현했습니다.

```kotlin
class QuerydslPagingItemReader<T : Any>(
    private val entityManagerFactory: EntityManagerFactory,
    name: String,
    pageSize: Int,
    private val queryProvider: (JPAQueryFactory) -> JPAQuery<T>,
) : AbstractPagingItemReader<T>()
```

페이지를 읽을 때 QueryDSL 쿼리에 offset/limit을 적용합니다.

```kotlin
results = queryProvider(queryFactory)
    .offset(page.toLong() * pageSize.toLong())
    .limit(pageSize.toLong())
    .fetch()
```

정산 Reader는 개별 Transaction을 애플리케이션에서 모두 합치지 않고 **DB에서 먼저 집계**합니다.

```kotlin
.where(tx.settlementDate.eq(settlementDate))
.groupBy(
    tx.settlementDate,
    payment.pgTid,
    payment.orderNo,
    order.id,
    order.orderAmount,
    payment.approvedAmount,
    payment.canceledAmount,
)
.orderBy(payment.pgTid.asc())
```

Processor에서 `orderRepository.findByOrderNo()`를 건별로 호출하지 않으므로 N+1 SELECT를 피합니다.

---

## 6. 부분취소 검증에서 중요한 점

아래 검증은 잘못된 방식입니다.

```kotlin
if (cancelAmount > approvalAmount) {
    // invalid
}
```

승인과 부분취소가 다른 정산일일 수 있기 때문입니다.

```text
9/12 승인       100,000
9/13 부분취소    30,000
```

9/13 데이터만 보면 승인 `0`, 취소 `30,000`이지만 정상 거래입니다.

따라서 취소 한도는 `PgPaymentEntity`의 **누적 금액**으로 검증합니다.

```kotlin
if (pgCanceledAmount > pgApprovedAmount) {
    INVALID_CANCEL_AMOUNT
}
```

즉 각 모델의 역할을 구분합니다.

```text
Transaction = 시점별 이벤트
Payment     = 누적 현재 상태
Settlement  = 정산일별 결과
```

---

## 7. 대사 상태

```kotlin
enum class PgSettlementStatus {
    MATCHED,
    ORDER_NOT_FOUND,
    PAYMENT_AMOUNT_MISMATCH,
    INVALID_CANCEL_AMOUNT,
}
```

| 상태 | 의미 |
|---|---|
| `MATCHED` | 주문과 PG 데이터가 정상적으로 일치 |
| `ORDER_NOT_FOUND` | PG에는 거래가 있으나 서비스 주문이 없음 |
| `PAYMENT_AMOUNT_MISMATCH` | 주문금액과 PG 최초 승인금액 불일치 |
| `INVALID_CANCEL_AMOUNT` | 누적 취소금액이 최초 승인금액 초과 |

---

## 8. 패키지 구조

```text
src/main/kotlin/com/example/pgsettlement
├── PgSettlementApplication.kt
├── batch
│   ├── common
│   │   └── QuerydslPagingItemReader.kt
│   └── settlement
│       ├── PgSettlementJobConfig.kt
│       ├── PgSettlementProcessor.kt
│       ├── PgSettlementReaderConfig.kt
│       └── PgSettlementSource.kt
├── domain
│   ├── order
│   │   ├── OrderEntity.kt
│   │   └── OrderRepository.kt
│   └── pg
│       ├── payment
│       │   ├── PgPaymentEntity.kt
│       │   └── PgPaymentStatus.kt
│       ├── settlement
│       │   ├── PgSettlementEntity.kt
│       │   ├── PgSettlementRepository.kt
│       │   └── PgSettlementStatus.kt
│       └── transaction
│           ├── PgTransactionEntity.kt
│           └── PgTransactionType.kt
└── infrastructure
    └── querydsl
        └── QuerydslConfig.kt
```

Batch 구현과 도메인 Entity를 분리해 정산 업무 변경이 Entity 구조에 불필요하게 전파되지 않도록 했습니다.

Docker 테스트 데이터 관련 파일은 별도로 분리되어 있습니다.

```text
docker
└── seed
    ├── bulk-seed.sql
    └── seed-if-empty.sh
```


---

## 9. Docker 실행 + 자동 Bulk Seed

`docker compose up -d`만 실행하면 다음 순서로 동작합니다.

```text
postgres 시작
    ↓
health check 통과
    ↓
Flyway migrate
    ↓
seed 서비스 실행
    ↓
orders / pg_payment / pg_transaction 비어 있음?
    ├─ YES → bulk insert
    └─ NO  → skip
```

실행:

```bash
docker compose up -d
```

기본값은 **PG 결제 100,000건**입니다.

```text
orders          100,000건
pg_payment      100,000건
pg_transaction  약 110,000건
```

`pg_transaction`이 더 많은 이유는 10% 정도의 결제에 부분취소 또는 전체취소 이벤트를 추가하기 때문입니다.

자동 seed는 PostgreSQL의 `generate_series`를 사용하므로 애플리케이션에서 `save()`를 반복 호출하는 것보다 대량 테스트 데이터를 훨씬 빠르게 준비할 수 있습니다.

### 데이터 개수 변경

환경변수로 조절합니다.

```bash
BULK_DATA_COUNT=1000000 docker compose up -d
```

또는 프로젝트 루트에 `.env`를 만들 수 있습니다.

```bash
cp .env.example .env
```

```dotenv
SEED_ENABLED=true
BULK_DATA_COUNT=1000000
SEED_BASE_DATE=2026-09-12
```

`SEED_BASE_DATE`는 승인 거래의 정산일입니다. 부분취소/전체취소 거래는 자동으로 다음 날에 생성됩니다.

### 자동 삽입 조건

다음 세 테이블에 source row가 하나도 없을 때만 bulk insert합니다.

```text
orders
pg_payment
pg_transaction
```

이미 데이터가 있다면:

```text
[seed] source data already exists (...) skip bulk insert.
```

처럼 종료합니다.

따라서 단순히 Docker를 재시작해도 기존 데이터가 중복 생성되지 않습니다.

```bash
docker compose restart
```

### 생성되는 테스트 케이스

Bulk 데이터에는 정상 건뿐 아니라 대사 오류 검증용 데이터도 일부 포함합니다.

| 규칙 | 결과 |
|---|---|
| 기본 데이터 | `MATCHED` |
| 매 10번째 | 부분취소 또는 전체취소 발생 |
| 매 20번째 | 전체취소 |
| 매 333번째 | 주문이 없는 PG 결제 (`ORDER_NOT_FOUND`) |
| 매 500번째 | 주문/PG 승인금액 불일치 (`PAYMENT_AMOUNT_MISMATCH`) |

승인 거래는 `SEED_BASE_DATE`, 취소 거래는 `SEED_BASE_DATE + 1일`로 생성됩니다.

### Seed 로그 확인

```bash
docker compose logs seed
```

예:

```text
[seed] source tables are empty. inserting 100000 payments...
[seed] completed.
orders         | 100000
pg_payment     | 100000
pg_transaction | 110000
pg_settlement  | 0
```

### 데이터를 비우고 다시 생성

Docker volume까지 완전히 삭제하고 다시 생성하려면:

```bash
docker compose down -v
BULK_DATA_COUNT=100000 docker compose up -d
```

테이블 구조는 유지한 채 source 데이터만 비우고 다시 seed하려면 다음처럼 실행할 수 있습니다.

```sql
TRUNCATE TABLE pg_settlement;
TRUNCATE TABLE pg_transaction RESTART IDENTITY;
TRUNCATE TABLE pg_payment RESTART IDENTITY;
TRUNCATE TABLE orders RESTART IDENTITY;
```

그 다음 seed 컨테이너를 다시 실행합니다.

```bash
docker compose run --rm seed
```

### 자동 Seed 비활성화

```bash
SEED_ENABLED=false docker compose up -d
```

### 관련 파일

```text
docker-compose.yml
docker/seed/seed-if-empty.sh
docker/seed/bulk-seed.sql
.env.example
```

`migrate` 서비스가 Spring Boot를 실행하지 않고도 동일한 Flyway migration SQL을 적용한 후, `seed` 서비스가 데이터를 넣습니다. 따라서 **Docker만 실행해도 DB 스키마와 테스트 원천 데이터가 준비**됩니다.

---

## 10. 소량 데모 데이터가 필요한 경우

기존 `demo-data.sql`도 유지되어 있습니다. 자동 Bulk Seed를 끄고 소량 데이터만 테스트할 때 사용합니다.

```bash
SEED_ENABLED=false docker compose up -d
```

그 다음:

```bash
psql \
  -h localhost \
  -U settlement \
  -d settlement \
  -f demo-data.sql
```

데모 데이터에는 승인, 부분취소, 주문 미존재 케이스가 포함되어 있습니다.

---

## 11. Batch 실행

이 압축파일에는 Gradle Wrapper 바이너리를 포함하지 않았으므로 Gradle이 설치된 환경에서 실행합니다.

기본 설정은 애플리케이션 기동만으로 Batch가 실행되지 않도록 `spring.batch.job.enabled=false`입니다. 정산 Job을 실행할 때만 활성화합니다.

```bash
gradle bootRun --args='--spring.batch.job.enabled=true settlementDate=2026-09-13'
```

Spring Boot가 `pgSettlementJob`을 실행하고 `settlementDate`를 JobParameter로 전달합니다.

9월 13일 예상 결과:

```text
PG-TID-001
  approvalAmount    = 0
  cancelAmount      = 30000
  feeAmount         = -900
  settlementAmount  = -29100
  status            = MATCHED

PG-TID-ORPHAN
  approvalAmount    = 50000
  cancelAmount      = 0
  feeAmount         = 1500
  settlementAmount  = 48500
  status            = ORDER_NOT_FOUND
```

결과 확인:

```sql
SELECT *
FROM pg_settlement
ORDER BY settlement_key;
```

---

## 12. 테스트

```bash
gradle test
```

`PgSettlementProcessorTest`에 다음 케이스가 포함되어 있습니다.

- 부분취소 + 수수료 환급 정산액 계산
- 주문이 없는 거래
- 누적 취소금액이 승인금액을 초과한 거래

---

## 13. 실무에서 Job을 더 분리한다면

현재 프로젝트는 **한 주문에 하나의 PG 결제가 대응하고 주문금액과 PG 최초 승인금액이 같아야 한다**는 단순화된 전제를 둡니다. 분할결제, 쿠폰/포인트 혼합결제, 복수 PG 결제는 별도 Payment Allocation 모델이 필요합니다.

현재 프로젝트는 정산 계산에 집중한 최소 예제입니다. 실제 서비스에서는 다음처럼 나누는 편이 좋습니다.

```text
pgImportJob
    │
    │ PG API / 파일 수집
    ▼
pg_transaction
    │
    ▼
pgReconciliationJob
    │
    │ 주문 ↔ PG 거래 대사
    ▼
pgSettlementJob
    │
    │ 승인 / 취소 / 수수료 정산
    ▼
pg_settlement
    │
    ▼
pgDepositReconciliationJob
    │
    │ 정산 예정액 ↔ 실제 입금액 대사
    ▼
최종 정산 완료
```

**결제 대사**와 **실제 입금 대사**는 별개 문제로 보는 것이 좋습니다.

---

## 14. 대용량 처리 시 개선점

현재 `QuerydslPagingItemReader`는 offset paging입니다.

```sql
OFFSET 0 LIMIT 1000
OFFSET 1000 LIMIT 1000
OFFSET 2000 LIMIT 1000
```

데이터가 수백만~수천만 건이고 배치 도중 원본 데이터가 변경될 수 있다면 다음 개선을 고려합니다.

### No-offset / Keyset Paging

```text
lastPgTid = PG-TID-1000

WHERE pg_tid > :lastPgTid
ORDER BY pg_tid
LIMIT 1000
```

장점:

- 뒤 페이지로 갈수록 느려지는 OFFSET 비용 감소
- 중간 데이터 삽입에 의한 paging 흔들림 감소

다만 현재 Reader는 `settlement_date + pg_tid`로 집계하는 GROUP BY Query이므로, keyset paging을 적용할 때 집계 결과의 유일한 정렬 키를 명확하게 설계해야 합니다.

### 원본 데이터 마감

가능하면 다음 순서가 단순합니다.

```text
PG 데이터 수집
      ↓
정산 대상 데이터 마감
      ↓
정산 Batch 실행
```

정산 중 같은 정산일 데이터가 계속 삽입되는 구조는 피하는 것이 좋습니다.

---

## 15. 실무 체크리스트

- PG 이벤트 ID에 UNIQUE 제약 적용
- 정산 결과에 멱등 키 적용
- 금액은 `Double`이 아닌 `BigDecimal` 사용
- 부분취소는 당일 승인액이 아닌 누적 승인/취소 상태로 검증
- Processor 내부 건별 Repository 조회 지양
- Paging 정렬키는 유일하고 deterministic하게 구성
- 재실행 시 같은 정산 결과가 중복 생성되지 않도록 설계
- PG 원천 데이터와 계산 결과를 분리
- 정산 예정액과 실제 입금액 대사를 별도 처리
- 대규모 데이터에서는 keyset/no-offset paging 검토

---

## 참고

- Spring Batch 6 API: `AbstractPagingItemReader`, `JpaItemWriter`, `JobBuilder`, `StepBuilder`
- QueryDSL JPA Jakarta artifact 사용
- 이 저장소는 학습/설계 예제로, 실제 PG사마다 승인/취소 이벤트 모델, 수수료 VAT, 면세/과세, 정산주기(D+N), 휴일 이월, 지급보류, 차지백 정책 등을 추가해야 합니다.
