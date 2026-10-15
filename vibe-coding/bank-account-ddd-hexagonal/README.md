# Bank Account — DDD + Hexagonal Architecture

Kotlin 2.2.21 / JDK 21 / Spring Boot 4.0.8 / Spring Data JPA / Querydsl 7 / PostgreSQL / Kafka / Kotest / MockK / Testcontainers.

## Module & bounded-context structure

- `domain`: pure business model and logical bounded contexts: Bank (`BankIdentity`), Account (`Account`, `AccountOwner`), Transfer (`TransferIntent`, `Journal`), and integration contract.
- `application`: use cases and inbound/outbound ports; transactional `BankService` orchestration.
- `infrastructure`: JPA/Querydsl persistence adapters, PostgreSQL transfer policy, outbox delivery, inbox consumer, audit adapter.
- `api`: REST inbound adapter, authentication, authorization, boot configuration.

Bounded contexts are **logically separated in a modular monolith**, not three independently deployed services. The existing `BankService` facade spans these contexts; extracting dedicated Bank/Account/Transfer use-case classes is a future architectural refinement.

## Run

Requires Java 21, installed Gradle and Docker. No Gradle wrapper is bundled.

```sh
docker compose up -d
gradle :api:bootRun
gradle test
```

Configuration: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `KAFKA_BOOTSTRAP_SERVERS`, `JWT_SECRET` (>=32 bytes), `DAILY_TRANSFER_LIMIT` (default 10000000.00).

## HTTP authorization

All `/api/v1/accounts/**` endpoints need Bearer **HS256 JWT**. `sub` equals account `ownerName`. Users can list/open/read/deposit/withdraw their own accounts and transfer from their own accounts to another account. Health endpoint is public. Use OIDC JWKS and immutable user IDs, not owner names, in production. Development JWT secret is insecure and must be overridden.

Local test JWT (Python 3 standard library):

```python
import base64, hashlib, hmac, json, time
secret = b'local-only-dev-secret-change-in-production-0123456789'
def enc(d):
    return base64.urlsafe_b64encode(json.dumps(d,separators=(',',':')).encode()).decode().rstrip('=')
a = enc({'alg':'HS256','typ':'JWT'})
b = enc({'sub':'Alice','iat':int(time.time()),'exp':int(time.time())+3600})
s = base64.urlsafe_b64encode(hmac.new(secret,f'{a}.{b}'.encode(),hashlib.sha256).digest()).decode().rstrip('=')
print(f'{a}.{b}.{s}')
```

API examples (use header `Authorization: Bearer <token>`):

```text
POST /api/v1/accounts  {"bankCode":"BANK001","accountNumber":"123","ownerName":"Alice"}
POST /api/v1/accounts/1/deposits  {"amount":100.00}
POST /api/v1/accounts/transfers  {"requestId":"aaaaaaaa-aaaa-4aaa-aaaa-aaaaaaaaaaaa","fromAccountId":1,"toAccountId":2,"amount":10.00}
```

## Reliability guarantees

- Request UUID unique constraint gives idempotent transfer replay and payload mismatch rejection.
- Ordered pessimistic account locks prevent overdraft and reduce deadlocks; daily limit atomically reserved in `daily_transfer_usage` UPSERT inside the same transaction.
- Journal debits and credits sum to zero; the postings and balance update roll back together.
- Kafka Transactional Outbox uses PostgreSQL `FOR UPDATE SKIP LOCKED`, committed worker lease (90 seconds), retry backoff, and a DLQ topic after 5 failures. Kafka delivery is **at-least-once**, not exactly once.
- Event consumer persists event ID to a unique inbox and audit row in one PostgreSQL transaction. Broker redeliveries are safe for this DB side effect.
- Audit logs record successful API actions and consumed events. **Limitations**: API action audit is executed after the use-case commits, rather than atomically; production-grade regulatory audit needs same-transaction recording. No DLQ replay service included. Daily limits are per account per PostgreSQL `CURRENT_DATE`.

## Tests

`gradle test`: Kotest, MockK, JUnit, PostgreSQL + Kafka Testcontainers, idempotency, balanced journal, concurrent transfers, daily limit rollback, consumer dedup, publishing, ownership unit test. Docker required. **Build and tests not executed in this environment** because Gradle/Docker are absent.


## Bounded contexts / Saga / Keycloak extension

- `bank-context`, `account-context`, `transfer-context` are independent Gradle modules with their own aggregate contracts. Existing single-database REST flow remains in the original four modules for backward compatibility; full adapter migration into the contexts is **not complete**.
- `TransferSagaCoordinator` is a framework-free compensating saga model. It requires a persistent, idempotent `TransferSagaPort` adapter and an asynchronous orchestrator before it is safe to use in production. The existing REST transfer still uses an atomic DB transaction, **not** the saga.
- Keycloak starts at `http://localhost:8081`, realm `bank`; local demo users `alice` / `bob` with passwords `alice-dev-password` and `bob-dev-password`. The API checks signed Keycloak JWT issuer, JWK, and audience `bank-api`. Demo passwords and admin account are **development only**.
- Since the issuer defaults to `localhost:8081` for a local host-run API, container-hosted deployments should set `JWT_ISSUER_URI` and `JWT_JWKS_URI` separately for their network routing.

### Validation commands (run on a machine with Gradle 8.14+ and Docker)

```bash
gradle clean test
docker compose up -d
gradle :api:bootRun
```

**Validation limitation:** source distribution generated in an environment without Gradle, Docker, Maven repository network access; the complete multi-module build and Testcontainers tests could not be run here.

## Final local validation / real HTTP E2E (2026-10)

On a development machine install **JDK 21**, **Gradle 8.14+**, **Docker Engine/Desktop with Compose v2**, and **Python 3**. Docker must be running; internet connectivity is required on first run to fetch Maven dependencies and PostgreSQL/Kafka/Keycloak images. Free ports: `5432`, `9092`, `8080`, `8081`.

```bash
bash verification/verify-local-e2e.sh
```

The script runs `gradle clean test :api:bootJar` (including PostgreSQL and Kafka Testcontainers), starts PostgreSQL/Kafka/Keycloak through Docker Compose, starts the Spring Boot API, checks `/actuator/health`, and runs `verification/api_e2e.py`. The E2E test obtains **real Keycloak access tokens** and checks unauthorized access, two account openings, cross-owner access denial, deposit, withdrawal, transfer, duplicate transfer request, idempotency conflict, transaction history, and final balances. It exits nonzero on any failure. Logs are written to `verification/results/` (excluded from ZIP). The containers remain running by default; set `E2E_STOP_COMPOSE=1` to tear them down when finished. `docker compose down` can also stop them manually.

The API's account ownership check compares `ownerName` with the OIDC `sub` (immutable Keycloak UUID), **not the visible username**. The E2E test therefore uses the verified token's `sub` as the owner name; a future model revision should rename the field to `ownerSubject`.

**Status note:** The source archive includes the runnable validation gate but full Gradle, Docker Testcontainers, and HTTP E2E execution are **not certified as passing** in this authoring environment (no Gradle/Docker available). See `VALIDATION.md`.

## Kotest BehaviorSpec

모든 Kotlin 테스트는 Kotest `BehaviorSpec`의 `Given` → `When` → `Then` 구조로 작성되었습니다. `api` 통합 테스트는 `SpringExtension`으로 Spring Context와 Testcontainers(PostgreSQL/Kafka)를 사용합니다.

```bash
gradle test
# 또는 Docker 실행 환경에서
bash verification/verify-local-e2e.sh
```

**검증 제한:** 이 ZIP은 BehaviorSpec 변환 결과물이며, 현재 환경에서는 Gradle 의존성 다운로드 및 Docker 실행이 불가능하여 전체 테스트 통과를 확인하지 못했습니다.

## Kotest BehaviorSpec 테스트 명명 규칙

모든 BehaviorSpec의 시나리오 이름과 설명은 한국어 문장으로 작성합니다. `Given`, `When`, `Then`은 Kotest 라이브러리가 제공하는 DSL 함수명이므로 그대로 유지합니다.

```kotlin
Given("출금 계좌에 충분한 잔액이 있을 때") {
    When("계좌이체를 실행하면") {
        Then("차변과 대변이 일치하는 원장 기록이 저장된다") {
            // 검증
        }
    }
}
```
