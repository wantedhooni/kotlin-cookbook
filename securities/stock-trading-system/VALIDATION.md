# Validation Report

## Validation date
2026-09-23

## Implemented in this revision

### 1. 정정주문
- LIMIT 가격 정정 API
- `order_corrections` 원주문/정정요청 관계 테이블
- `CORRECTION_REQUESTED` 상태
- Order Router → Mock KRX 정정 요청 Topic
- Mock KRX early correction / 부분체결 후 정정 / 완전체결 후 reject
- 정정 승인과 체결 이벤트의 Kafka cross-topic 순서 역전 대응
- BUY 가격 인상 제한으로 기존 증거금 예약 초과 방지

### 2. D+2 Settlement
- 신규 `settlement-service`
- 체결별 Settlement Obligation 저장
- Asia/Seoul 기준 거래일 산출
- Weekend + 주입형 Holiday 영업일 Calendar
- T+2 영업일 결제일 산출
- BUY: 주문예약 → settlement hold → 결제일 예수금/신용 확정
- SELL: 체결대금 → pending receivable → 결제일 예수금 반영
- 정산예정 지급액/수취액/미수금 조회
- Scheduler 및 테스트용 강제 결제 API
- Settlement Kafka Retry/DLT 설정

### 3. Performance / Observability / Autoscaling
- k6 장 시작 Burst 시나리오
- k6 주문/조회 혼합 시나리오
- load 계좌 seed script
- Prometheus scrape 설정
- Grafana datasource/dashboard provisioning
- HTTP p95, 5xx, CPU, DB pool dashboard
- Kubernetes HPA `autoscaling/v2`
- HPA 계산을 위한 CPU/Memory requests/limits
- Capacity planning 문서

### 기존 resilience 테스트 유지
- PostgreSQL Testcontainers 계좌 동시성 테스트
- 동일 계좌 BUY over-commit 방지
- cancel-before-late-execution 정합성 테스트
- 신규 D+2 Account settlement 통합 테스트
- Kafka Testcontainers duplicate execution 테스트
- malformed payload Retry → DLT 테스트

## Checks executed

### Passed

```text
./scripts/verify-project.sh
[1/6] Required business files
[2/6] Domain/event compilation
[3/6] Flyway migrations
[4/6] Testcontainers/DLT test sources
[5/6] YAML / JSON
YAML/JSON parse OK
[6/6] Gradle tests
Skipped. RUN_GRADLE_TESTS=1 ./scripts/verify-project.sh
Project verification completed.
```

Additional checks:

- 모든 `common-domain` production Kotlin source를 실제 `kotlinc` 컴파일
- 모든 `common-event` source를 compiled common-domain classpath로 실제 `kotlinc` 컴파일
- 정정주문 경합 + BUY settlement hold + 영업일 T+2를 실행하는 standalone domain smoke test 통과: `DOMAIN_SMOKE_OK`
- 변경된 service source를 dependency 없는 Kotlin compiler로 parse 시도했으며 syntax-error 패턴(`expecting`, `syntax error`) 없음
- Kubernetes/Application/Docker Compose YAML 파싱 성공
- Grafana dashboard JSON 파싱 성공
- 임시 Gradle distribution/cache는 최종 ZIP에서 제거

## Full Gradle/Testcontainers execution

전체 Gradle 테스트도 실행을 시도했지만 이 sandbox는 Gradle distribution 서버 DNS 접근이 차단되어 첫 bootstrap에서 중단되었습니다.

```text
Gradle 8.10.2 not found locally; downloading official distribution...
curl: (6) Could not resolve host: services.gradle.org
```

따라서 Spring Boot/JPA/Testcontainers 전체 suite가 이 환경에서 실행됐다고 주장하지 않습니다. 네트워크와 Docker가 가능한 개발 환경에서 다음 명령으로 검증합니다.

```bash
RUN_GRADLE_TESTS=1 ./scripts/verify-project.sh
```

또는:

```bash
./gradlew clean test
```

## Project size at packaging

```text
Kotlin source: 2,396 lines
Kotlin files: 62
Flyway migration files: 8
```
