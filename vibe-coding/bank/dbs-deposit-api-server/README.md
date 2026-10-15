# DBS Deposit API Adapter Server

Kotlin + Spring Boot + JPA 기반의 DBS Deposit API 어댑터 예제입니다. 원격 DBS 예금계좌 조회 결과를 로컬 DB에 동기화하고, 읽기 API는 저장된 스냅샷을 제공합니다. 동기화 API에는 **멱등성(Idempotency)** 과 **다중 인스턴스 동시성 제어**가 포함되어 있습니다.

## 1. DBS Deposit API 분석

참조 문서:

- https://www.dbs.com/dbsdevelopers/discover/deposit.html#section/Overview
- https://www.dbs.com/dbsdevelopers/discover/index.html

공개 Discover 페이지에서 확인되는 Deposit API의 목적은 고객의 Deposit Account 목록과 Savings / Multiplier / Multi-Currency 계좌 상세를 조회하는 것입니다.

DBS 개발자 포털의 동일 계열 API 문서(Parties, Fund Transfer, Reference Data)는 다음 공통 헤더 패턴을 사용합니다.

- `clientId`: Client Identifier
- `accessToken`: OAuth2에서 발급된 JWT access token
- `accept-version`: 응답 메시지 버전
- `uuid`: 요청별 message UID

### 중요: 원격 path/schema 확인 필요

현재 공개 `deposit.html` 페이지는 상세 OpenAPI 내용이 동적으로 렌더링되어, 비로그인 정적 페이지에서 endpoint/schema 전체를 안정적으로 추출할 수 없습니다. 따라서 이 샘플은 **DBS 호출 path를 환경변수로 분리**했습니다.

기본값은 다음과 같이 두었지만, 실제 DBS Sandbox 계정에서 보이는 문서 값과 다르면 환경변수만 바꾸면 됩니다.

```text
DBS_LIST_DEPOSITS_PATH=/api/sg/v1/parties/{partyId}/deposits
DBS_DEPOSIT_DETAIL_PATH=/api/sg/v1/deposits/{accountId}
```

또한 DBS 응답 필드명이 달라도 `DbsJsonNormalizer` 한 곳에서 매핑을 조정하도록 했습니다. 원본 JSON은 `raw_json`에 보존합니다.

## 2. 기술 스택

- Java 21
- Kotlin 2.2.21
- Spring Boot 3.5.11
- Spring Web / RestClient
- Spring Data JPA / Hibernate
- Flyway
- H2 (기본 로컬 실행)
- PostgreSQL (운영형 프로필)
- JUnit 5 / Spring Boot Test

## 3. 제공 API

### DBS -> 로컬 DB 동기화

```http
POST /api/v1/parties/{partyId}/deposits/sync
Idempotency-Key: <unique-key>
```

응답 예시:

```json
{
  "partyId": "party-1001",
  "receivedCount": 2,
  "insertedCount": 2,
  "updatedCount": 0,
  "syncedAt": "2026-10-02T07:30:00Z"
}
```

응답 헤더:

```text
Idempotent-Replayed: false
```

같은 `Idempotency-Key` + 같은 요청을 재전송하면 원격 DBS를 다시 호출하지 않고 저장된 응답을 돌려주며 `Idempotent-Replayed: true`가 됩니다.

### 저장된 예금계좌 목록 조회

```http
GET /api/v1/parties/{partyId}/deposits
```

### 저장된 특정 예금계좌 조회

```http
GET /api/v1/parties/{partyId}/deposits/{accountId}
```

### 원격 DBS 상세 조회 passthrough

```http
GET /api/v1/provider/deposits/{accountId}
```

이 endpoint는 캐시를 갱신하지 않습니다.

## 4. 멱등성 설계

`idempotency_record` 테이블에 아래 정보를 저장합니다.

- operation
- idempotency key
- SHA-256 request hash
- `PROCESSING / SUCCEEDED` 중심의 처리 상태 (`FAILED` enum은 확장용)
- HTTP status
- response body
- retention expiration

DB unique constraint:

```text
UNIQUE(operation, idempotency_key)
```

처리 규칙:

1. 최초 key는 `PROCESSING`으로 선점합니다.
2. 같은 key + 같은 요청이 동시에 들어오면 `409 IDEMPOTENCY_IN_PROGRESS`를 반환합니다.
3. 성공 완료 후 같은 요청이 다시 오면 저장한 결과를 replay합니다.
4. 외부 호출/동기화가 실패하면 해당 key 예약을 제거하여 안전하게 재시도할 수 있게 합니다. 이 API는 upstream이 조회 API이므로 재시도가 부작용을 만들지 않습니다.
5. 같은 key를 다른 `partyId`에 재사용하면 request hash가 달라 `422 IDEMPOTENCY_KEY_REUSED`를 반환합니다.
6. `PROCESSING`이 비정상 종료로 남아도 기본 1분 후 stale reservation으로 간주하여 새 요청이 선점할 수 있습니다.

실제 금융성 write API로 확장할 때도 동일 패턴을 사용할 수 있습니다. 단, 외부 금융기관이 자체 idempotency/reference key를 지원한다면 내부 key와 함께 upstream에도 전달하는 것이 더 안전합니다.

## 5. 동시성 설계

### 문제

서버가 여러 대일 때 서로 다른 `Idempotency-Key`로 같은 `partyId` 동기화를 동시에 시작할 수 있습니다.

### 적용 방식: DB lease

`sync_lease` 테이블에 `party_id`, `locked_until`, `locked_by`를 저장합니다. 동기화 시작 시 아래 의미의 atomic update로 lease를 획득합니다.

```sql
UPDATE sync_lease
SET locked_until = :now + lease,
    locked_by = :owner
WHERE party_id = :partyId
  AND (locked_until IS NULL OR locked_until < :now)
```

한 요청만 1 row를 갱신할 수 있고, 나머지는 `409 SYNC_ALREADY_RUNNING`을 받습니다.

이 방식은 외부 HTTP 호출 동안 JPA pessimistic lock과 DB transaction을 계속 열어두지 않는 장점이 있습니다. 프로세스가 비정상 종료되어도 `locked_until` 이후 자동 회복됩니다.

기본값:

```text
DBS read timeout = 5s
sync lease = 30s
```

운영에서는 **lease 시간이 최대 원격 호출 시간보다 충분히 길어야 합니다.**

### 추가 DB 안전장치

예금계좌 테이블:

```text
UNIQUE(party_id, external_account_id)
```

동기화 lease가 1차 방어선이고, DB unique constraint가 최종 중복 방어선입니다.

## 6. 실행 - 가장 간단한 방법(mock + H2)

DBS credential 없이 실행할 수 있도록 기본값은 mock provider입니다.

Gradle 8.14.x 이상이 설치되어 있다면:

```bash
gradle bootRun
```

테스트:

```bash
gradle test
```

동기화 호출:

```bash
curl -i -X POST \
  http://localhost:8080/api/v1/parties/party-1001/deposits/sync \
  -H 'Idempotency-Key: 8cd9c742-8e82-4fb8-b3fc-1be17320ff2f'
```

조회:

```bash
curl http://localhost:8080/api/v1/parties/party-1001/deposits
```

## 7. PostgreSQL 실행

```bash
docker compose up -d postgres
```

환경변수:

```bash
export SPRING_PROFILES_ACTIVE=postgres
export DB_URL=jdbc:postgresql://localhost:5432/dbs_deposit
export DB_USERNAME=dbs
export DB_PASSWORD=dbs

gradle bootRun
```

## 8. 실제 DBS Sandbox 연결

`.env.example`을 참고해 설정합니다.

```bash
export DBS_MODE=http
export DBS_BASE_URL=https://www.dbs.com/sandbox
export DBS_CLIENT_ID='<client-id>'
export DBS_ACCESS_TOKEN='<jwt-access-token>'
export DBS_ACCEPT_VERSION='application/json;v=1.0'

# 실제 Deposit 문서에 맞게 조정
export DBS_LIST_DEPOSITS_PATH='/api/sg/v1/parties/{partyId}/deposits'
export DBS_DEPOSIT_DETAIL_PATH='/api/sg/v1/deposits/{accountId}'
```

그 다음:

```bash
gradle bootRun
```

DBS access token 발급은 Authorization Code/OAuth 절차와 사용자 인증이 관여하므로 이 프로젝트에서는 **이미 발급받은 access token을 주입**하는 형태로 분리했습니다. 운영에서는 별도 Token Provider를 구현해 만료 전 갱신/재인증 정책을 적용하십시오.

## 9. DBS 응답 스키마가 다를 때

수정 위치:

```text
src/main/kotlin/com/example/dbsdeposit/client/DbsJsonNormalizer.kt
```

현재 인식하는 대표 후보명:

- 목록: `depositAccounts`, `accounts`, `deposits`, 또는 root array
- 계좌 ID: `accountId`, `depositAccountId`, `id`, `accountIdentifier`
- 계좌번호: `maskedAccountNumber`, `accountNumber`, `displayAccountNumber`
- 상품: `productName`, `accountName`, `productType`, `accountType`
- 잔액: `availableBalance`, `balance`, `currentBalance`
- 통화: `currency`, `currencyCode`

실제 DBS JSON 샘플을 확보하면 이 부분을 강타입 DTO로 고정하는 것을 권장합니다.

## 10. 실패/재시도 정책

- DBS 4xx/5xx 및 네트워크 오류 -> `502 DBS_PROVIDER_ERROR`
- 동일 idempotency 요청 처리 중 -> `409 IDEMPOTENCY_IN_PROGRESS`, `Retry-After: 1`
- 같은 party 동기화 중 -> `409 SYNC_ALREADY_RUNNING`, `Retry-After: 1`
- 다른 요청에 같은 Idempotency-Key 재사용 -> `422 IDEMPOTENCY_KEY_REUSED`

현재 샘플은 upstream이 **조회 API**이므로 실패한 idempotency reservation을 제거해 재시도를 허용합니다. 실제 송금/결제 같은 금융 write API에서는 네트워크 타임아웃 직후 단순 재호출하면 중복 거래 위험이 있으므로, upstream idempotency/reference key 전달 + 거래상태 조회 후 재시도 정책으로 변경해야 합니다.

## 11. 운영 적용 시 추가 권장사항

- access token을 환경변수 평문 대신 Vault/KMS/Secret Manager로 관리
- 민감한 계좌번호/원본 JSON 암호화 또는 최소 저장
- API 인증/인가(Spring Security, mTLS 등) 추가
- Micrometer + Prometheus/Grafana 지표 구성
- traceId / correlationId 로깅
- idempotency 만료 데이터 정리 batch
- DBS rate limit에 맞춘 retry/backoff + circuit breaker
- upstream API가 pagination을 제공하면 전체 page 완료 후 원자적 snapshot 전환
- 실제 Deposit OpenAPI를 확보한 뒤 `JsonNode` 기반 어댑터를 강타입 DTO로 치환

## 12. 주요 파일

```text
src/main/kotlin/com/example/dbsdeposit/
├── api/
│   ├── DepositController.kt
│   ├── ProviderController.kt
│   └── ApiExceptionHandler.kt
├── client/
│   ├── DbsDepositClient.kt
│   ├── HttpDbsDepositClient.kt
│   ├── MockDbsDepositClient.kt
│   └── DbsJsonNormalizer.kt
├── domain/
│   ├── DepositAccountEntity.kt
│   ├── IdempotencyRecordEntity.kt
│   └── SyncLeaseEntity.kt
├── repository/
└── service/
    ├── DepositSyncService.kt
    ├── IdempotencyService.kt
    ├── SyncLeaseService.kt
    └── DepositPersistenceService.kt
```

동시성/멱등성 상세 설명은 `docs/ARCHITECTURE.md`도 참고하십시오.
