# 데이터 모델 명세

## 1. 계정 테이블: `accounts`

| 컬럼 | 데이터 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| `id` | `UUID` | 기본 키(PK) | 계정 식별자 |
| `login_id` | `VARCHAR(50)` | NOT NULL, UNIQUE | 소문자 정규화 로그인 ID |
| `email` | `VARCHAR(255)` | NOT NULL, UNIQUE | 소문자 정규화 이메일 |
| `display_name` | `VARCHAR(100)` | NOT NULL | 표시 이름 |
| `role` | `VARCHAR(20)` | `USER`, `ADMIN` | 관리 대상 계정의 역할 메타데이터 |
| `status` | `VARCHAR(20)` | `ACTIVE`, `SUSPENDED`, `DELETED` | 계정 상태 |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | 최초 생성 일시 |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL | 최종 수정 일시 |

**스키마 정의 파일:** `src/main/resources/db/migration/V1__create_accounts.sql`

## 2. 상태 전이 규칙

```text
[계정 생성] ──▶ ACTIVE ◀────▶ SUSPENDED
                  │               │
                  └──────┬────────┘
                         ▼
                       DELETED
                    (종료 상태)
```

- 최초 상태: `ACTIVE`.
- 상태 변경 API로 `ACTIVE ↔ SUSPENDED` 전환이 가능하다.
- `DELETE` 요청으로 `ACTIVE` 또는 `SUSPENDED`에서 `DELETED`로 전환한다.
- 이 초기 프로젝트에서 `DELETED`는 복구할 수 없다. 상세 조회 및 일반 변경 API 대상에서도 제외한다.
- 역할 변경은 별도 환경변수에 지정한 관리자 인증 권한에 영향을 주지 않는다.
- 소프트 삭제 이후에도 `login_id`, `email`의 UNIQUE 제약은 유지된다.

## 3. Kafka 감사 이벤트

| 항목 | 값 |
| --- | --- |
| 토픽 | `accounts.audit.v1` |
| 메시지 키 | 계정 UUID 문자열 |
| 메시지 형식 | JSON |
| 전송 시점 | DB 트랜잭션 커밋 후 비동기 발행 |

**JSON 필드**
- `accountId`: 변경된 계정의 UUID
- `action`: 수행한 변경 종류
- `performedBy`: 변경을 요청한 관리자 식별자
- `occurredAt`: 이벤트 생성 일시

**`action` 허용값**
- `ACCOUNT_CREATED`: 계정 생성
- `ACCOUNT_UPDATED`: 정보 수정
- `ROLE_CHANGED`: 역할 변경
- `STATUS_CHANGED`: 상태 변경
- `ACCOUNT_DELETED`: 논리 삭제

> Kafka 이벤트는 감사 이력의 영구 보존을 보장하지 않는다. 전송 유실 방지가 필요하면 Transactional Outbox 등의 후속 설계를 적용해야 한다.
