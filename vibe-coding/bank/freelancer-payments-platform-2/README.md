# Freelancer Payments Platform

프리랜서·개인 판매자를 위한 **결제 → VAN 승인 → 정산 → 은행 지급 → 3.3% 원천징수/환급 추정** 흐름을 한 프로젝트에서 재현한 백엔드 포트폴리오입니다.

> 기술 스택: Spring Boot 4.0.8, Kotlin 2.2.21, JPA/Hibernate, PostgreSQL, Kotest 6(BDD), WireMock, k6, Docker Compose, Java 21

## 1. 이 프로젝트가 보여주는 것

일반적인 주문 CRUD가 아니라 금융/결제 백엔드에서 실제로 문제가 되는 **외부 승인과 내부 원장의 정합성**을 주제로 삼았습니다.

- 카드 결제: API → PG 역할의 본 서비스 → Mock VAN → Mock 카드사 승인 응답
- 망취소: VAN 승인이 끝난 뒤 내부 DB 확정이 실패하면 cancellation 전문을 보내 보상
- 멱등성: `Idempotency-Key` unique constraint로 중복 결제 방지
- 정산: 승인 결제를 seller 단위 지급채무로 변환
- 원장: `PG_CLEARING` 차변과 `SELLER / PLATFORM_FEE / WITHHOLDING_TAX` 대변의 합계를 항상 일치
- 지급: Mock Bank로 이체하고 지급 성공/실패를 별도 상태로 관리
- 세금: 3.3% 원천징수 모델과 단순 환급 추정 API
- 장애/부하: 내부 확정 장애 플래그, WireMock 외부계, k6 100 RPS 시나리오

## 2. 아키텍처

```text
Client
  |
  v
Payment API (PG role)
  |  1) create REQUESTED + idempotency key
  v
Mock VAN ----> Mock Card Issuer (conceptual)
  |  2) approve
  v
Payment confirm transaction
  |  success                    | failure after VAN approval
  |                             v
  |                         network cancellation
  v
APPROVED
  |
  v
Settlement Batch -> Ledger -> Mock Bank -> PAID
                    |
                    +-> withholding 3.3% / platform fee / seller payable
```

`PaymentService`에는 의도적으로 장시간 DB 트랜잭션을 잡은 채 VAN HTTP 호출을 하지 않습니다. 내부 상태 변경은 `PaymentTxService`의 짧은 트랜잭션으로 분리하고 외부 호출을 그 사이에 둡니다.

## 3. 결제 상태와 망취소

```text
REQUESTED --VAN decline----------> FAILED
REQUESTED --VAN network error----> FAILED
REQUESTED --VAN approved---------> APPROVED
REQUESTED --approved + DB fail---> VAN CANCEL -> CANCELED
                                      |
                                      +-- cancel fail -> FAILED / reconciliation required
```

핵심은 **VAN 승인 성공을 DB rollback만으로 되돌릴 수 없다는 점**입니다. 외부 시스템의 side effect는 별도의 취소 전문으로 보상해야 합니다. 실제 운영에서는 취소 실패 건을 outbox/retry/reconciliation queue로 넘기는 확장이 필요합니다.

## 4. 원장/이체 정합성

정산 100,000원, 플랫폼 수수료 3%, 원천징수 3.3% 예시:

| Account | Direction | Amount |
|---|---:|---:|
| PG_CLEARING | Debit | 100,000 |
| SELLER payable | Credit | 93,700 |
| PLATFORM_FEE | Credit | 3,000 |
| WITHHOLDING_TAX | Credit | 3,300 |

`Debit = Credit = 100,000`이 정합성 invariant입니다. `/api/reconciliation/ledger-imbalances`는 이 규칙이 깨진 settlement를 찾습니다.

은행 이체는 settlement와 분리된 외부 side effect입니다. 같은 settlement가 이미 `PAID`이면 재호출하지 않는 형태로 기본적인 지급 멱등성을 둡니다. 실서비스에서는 은행의 거래고유번호와 조회 API를 이용한 사후 대사가 추가되어야 합니다.

## 5. 세금 모델

`TaxCalculator`는 포트폴리오 데모를 위해 3.3%를 `소득세 3.0% + 지방소득세 0.3%`의 원천징수 모델로 단순화합니다.

```text
gross income             1,000,000
withholding (3.3%)          33,000
assumed final tax (2.0%)    20,000
estimated refund            13,000
```

환급은 `기납부 원천징수 - 가정 최종세액`의 단순 추정입니다. 실제 종합소득세/경정청구는 소득 구분, 필요경비, 공제, 타 소득, 신고내역 및 적용 법령에 따라 달라지므로 이 프로젝트의 계산값은 세무 판단용이 아닙니다.

## 6. 실행

필수: Docker / Docker Compose

```bash
docker compose up --build
```

서비스:

```text
App       http://localhost:8080
Mock VAN  http://localhost:18081
Mock Bank http://localhost:18082
Postgres  localhost:5432
```

결제:

```bash
curl -X POST http://localhost:8080/api/payments \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: pay-001' \
  -d '{"sellerId":"seller-1","amount":100000,"cardToken":"tok_demo"}'
```

망취소 시뮬레이션:

```bash
curl -X POST http://localhost:8080/api/payments \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: pay-compensation-001' \
  -d '{"sellerId":"seller-1","amount":100000,"cardToken":"tok_demo","simulateInternalFailureAfterVanApproval":true}'
```

정산 생성 및 지급:

```bash
curl -X POST http://localhost:8080/api/settlements/run
curl -X POST http://localhost:8080/api/settlements/{settlementId}/payout
```

세금/환급 추정:

```bash
curl 'http://localhost:8080/api/tax/estimate?grossIncome=1000000&assumedEffectiveFinalTaxRate=0.020'
```

## 7. Kotest BDD

```bash
gradle test
```

주요 시나리오:

- `PaymentServiceSpec`: VAN 승인 후 내부 확정 실패 → 망취소가 정확히 1회 호출됨
- `PaymentServiceSpec`: 승인 완료된 멱등키 재요청 → VAN 재승인 없음
- `SettlementInvariantSpec`: 정산 원장의 debit/credit 합계 불변식
- `TaxCalculatorSpec`: 3.3% 원천징수와 단순 환급 추정

## 8. 부하 테스트

```bash
# 앱을 먼저 실행
docker compose up --build -d

# 60초간 100 req/s 도착률
docker compose --profile load run --rm k6
```

`loadtest/payment.js` 기본 목표:

```text
arrival rate:     100 req/s
run time:          60 sec
expected calls: ~6,000
error rate:       < 1%
p95 latency:      < 300 ms
p99 latency:      < 700 ms
```

### 포트폴리오에 기재할 성능 수치

이 ZIP을 생성한 환경에는 Docker/k6 런타임이 없어 실제 벤치마크를 실행하지 않았습니다. 따라서 아래 숫자를 **실측값으로 주장하지 않습니다**. 실행 후 k6 결과로 반드시 교체하십시오.

예시 표기 형식:

| Metric | 목표 | 실측 입력란 |
|---|---:|---:|
| 처리율 | 100 req/s | `___ req/s` |
| 실패율 | < 1% | `___ %` |
| p50 | - | `___ ms` |
| p95 | < 300 ms | `___ ms` |
| p99 | < 700 ms | `___ ms` |

면접에서는 단순히 숫자만 말하지 말고 **DB connection pool, VAN latency, unique-key contention, transaction boundary** 중 무엇이 병목인지 설명하는 것이 핵심입니다.

## 9. 장애 시나리오

### A. VAN 승인 후 내부 확정 실패

요청에 `simulateInternalFailureAfterVanApproval=true`를 넣으면 VAN 승인을 받은 다음 내부 확정에서 예외가 발생합니다. 서비스는 VAN cancellation을 호출한 후 상태를 `CANCELED`로 기록합니다.

### B. VAN 장애

WireMock의 `authorize.json` 응답을 500 또는 delay로 바꾸어 테스트할 수 있습니다.

예: response에 다음을 임시 추가:

```json
"fixedDelayMilliseconds": 1500
```

운영 확장에서는 connect/read timeout, retry 제한, circuit breaker, cancellation outbox가 필요합니다.

### C. 은행 지급 실패

`wiremock/bank/mappings/transfer.json`을 `accepted:false`로 바꾸면 settlement가 `PAYOUT_FAILED`가 됩니다. 지급 retry 시에는 은행 조회를 먼저 수행해 중복 이체를 방지하는 설계가 필요합니다.

## 10. 도메인 학습 포인트

### PG–VAN–카드사

이 샘플에서 API 서버는 PG의 일부 역할, WireMock은 VAN을 모델링합니다. 실제 결제에서는 승인/취소 전문 포맷, 카드사 응답코드, timeout, 중복전문, 전문 추적번호가 중요합니다. 특히 외부 승인 이후 내부 장애에는 DB rollback이 아니라 **망취소/대사**가 필요합니다.

### 정산 주기

현재 `/settlements/run`은 수동 batch endpoint입니다. 실제 T+N 정산에서는 거래일/영업일, 취소/부분취소, 지급보류, 수수료/VAT, 정산 확정 cutoff를 별도 모델링하는 것이 좋습니다.

### 금융 원장과 이체 정합성

원장은 잔액을 직접 덮어쓰기보다 변경 사실을 entry로 남겨 합계로 검증하기 쉽도록 구성했습니다. 외부 은행 이체와 DB 원장은 하나의 ACID transaction이 될 수 없으므로 고유키, 상태머신, 대사(reconciliation)가 중요합니다.

### 전자금융감독규정·망분리

본 프로젝트는 학습용 로컬 환경이라 실제 망분리를 구현하지 않습니다. 포트폴리오 설명에서는 인터넷 구간/VAN 연계 구간/내부 DB 구간의 네트워크 경계, secret 분리, 최소권한, 감사로그, 운영자 접근통제를 별도 아키텍처 항목으로 제시하는 것이 적절합니다. 적용 의무와 구체적 요건은 사업자 유형과 최신 규정을 확인해야 합니다.

## 11. 면접에서 깊게 설명할 5개 질문

1. 왜 VAN 호출을 DB transaction 안에서 하지 않았는가?
2. VAN 승인은 성공했는데 DB commit이 실패하면 어떻게 복구하는가?
3. 같은 `Idempotency-Key` 요청이 동시에 100개 들어오면 무엇이 최종 방어선인가?
4. 은행 이체 timeout 때 재시도 전에 왜 거래 조회/대사가 필요한가?
5. 원장 debit/credit invariant가 깨졌을 때 자동 지급을 계속해야 하는가?

## 12. 다음 단계

실서비스 수준으로 확장하려면 아래가 우선순위입니다.

- Flyway migration + append-only ledger
- Outbox + Kafka로 결제/정산 이벤트 전달
- Resilience4j timeout/circuit breaker/retry policy
- 지급/취소 retry worker와 dead-letter queue
- 카드 부분취소·전체취소·refund ledger
- seller별 T+N cutoff와 영업일 캘린더
- Testcontainers PostgreSQL 통합 테스트
- Prometheus/Grafana dashboard와 trace ID
- 개인정보/카드정보 tokenization 및 secrets manager

## Disclaimer

이 코드는 포트폴리오/학습용입니다. 실제 카드 결제, 금융기관 연계, 세금 신고, 전자금융 규제 준수를 위한 완성 구현이 아닙니다.
