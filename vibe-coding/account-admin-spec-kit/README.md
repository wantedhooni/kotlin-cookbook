# Account Admin — GitHub Copilot + Spec Kit

Kotlin(Java 21) 계정 관리 백엔드와 프레임워크 없는 한국어 Admin UI를 포함한 **Spec-Driven Development 시작 프로젝트**입니다.
관리 계정 데이터와 로그인 인증은 별개입니다. 관리자 UI/API는 환경변수의 *단일 bootstrap 관리자*로 HTTP Basic 인증하며, `accounts.role=ADMIN` 레코드만으로는 API 로그인이 되지 않습니다.

**한글 스펙 요약:** [`specs/001-account-admin/overview-ko.md`](specs/001-account-admin/overview-ko.md)  
**한글화 범위:** Spec Kit 헌장·요구사항·구현 계획·작업 목록·데이터 모델·기술 검토·수동 테스트·OpenAPI 설명·Copilot 지침·Spring Boot 4 마이그레이션 문서.  
**유지 항목:** REST 경로, JSON 필드명, enum 값, DB 테이블/컬럼명, Kotlin 소스 및 Docker 설정.

## 기술 스택

| 구분 | 기술 |
| --- | --- |
| 서버 | Java 21, Kotlin 2.3.20, Spring Boot 4.1.1, Gradle 8.14.3 |
| 영속성/검색 | Spring Data JPA, OpenFeign Querydsl 7.1 (Jakarta), PostgreSQL 17, Flyway |
| 캐시 | Spring Cache + Redis 7 (상세 조회 TTL 5분) |
| 이벤트 | Spring Kafka + Apache Kafka 4.1 (트랜잭션 커밋 후 감사 이벤트) |
| 테스트 | Kotest 6.1.11 BehaviorSpec (BDD), MockK 1.14.9, Spring MVC Test |
| 관리 화면 | Spring Boot static HTML/CSS/Vanilla JS (추가 빌드 없음) |
| 실행 | Docker Compose, Dockerfile |
| 명세 | `.specify/memory/constitution.md`, `specs/001-account-admin/`, `.github/copilot-instructions.md` |


## Spring Boot 4.1.1 마이그레이션 참고

변경 내역 별도 문서: [`MIGRATION_SPRING_BOOT_4.md`](MIGRATION_SPRING_BOOT_4.md)

이 프로젝트는 기존 Spring Boot 3.5.7 버전을 **Spring Boot 4.1.1 (Spring Framework 7 / Jakarta EE 11)**로 교체했습니다. Java 21을 유지하고 Kotlin 2.3.20을 사용합니다.

| 항목 | Boot 3 구성 | Boot 4 구성 |
| --- | --- | --- |
| Web MVC | `spring-boot-starter-web` | `spring-boot-starter-webmvc` |
| Kafka | `spring-kafka` | `spring-boot-starter-kafka` |
| Flyway | `flyway-core` 단독 | `spring-boot-starter-flyway` + PostgreSQL 확장 |
| JSON | Jackson 2 `com.fasterxml.jackson.module` | Jackson 3 `tools.jackson.module:jackson-module-kotlin` |
| Kafka JSON | `JsonSerializer` (Jackson 2, deprecated) | `JacksonJsonSerializer` (Jackson 3) |
| Querydsl | `com.querydsl:querydsl-jpa:5.1.0:jakarta` | OpenFeign `io.github.openfeign.querydsl:querydsl-jpa:7.1` |
| Querydsl APT | `:jakarta` 분류자 | OpenFeign APT `:jpa` 분류자 (`kapt`) |
| 테스트 | `spring-boot-starter-test`, Kotest 5 | `spring-boot-starter-webmvc-test`, `spring-boot-starter-security-test`, Kotest 6.1.11 |

Spring Boot 관리 BOM을 통해 Hibernate, Spring Data, Kafka, Jackson의 세부 버전을 통합 관리합니다. Querydsl / Kotest / MockK만 명시적으로 버전을 지정합니다.

**업그레이드 검증 절차**: 네트워크가 연결된 Java 21 개발 환경에서 `./gradlew clean test bootJar`을 실행한 다음 `docker compose up --build -d` 및 `scripts/smoke-test.sh`를 수행하세요. 이 ZIP의 생성 환경은 외부 Maven/Gradle 저장소에 접근할 수 없어 실제 컴파일·통합 테스트는 수행하지 못했습니다.

**관련 문서**: [Spring Boot 4 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide), [Spring Data JPA Querydsl](https://docs.spring.io/spring-data/jpa/reference/repositories/core-extensions.html), [Spring Kafka 4 JSON](https://docs.spring.io/spring-kafka/reference/appendix/change-history.html).

## 제공 기능

- 계정 생성, ID 상세 조회, 검색/필터/페이지 조회 (Querydsl)
- 이메일·표시 이름 변경, `USER`/`ADMIN` 역할 변경, `ACTIVE`/`SUSPENDED` 상태 변경
- 삭제 시 `DELETED`로 **소프트 삭제**, 기본 목록과 상세 조회에서 제외 (고유 로그인 ID/이메일 계속 예약됨)
- `accounts` Redis 상세 캐시: 쓰기 요청 시 해당 ID 캐시 제거
- 계정 변경 후 Kafka `accounts.audit.v1` 토픽으로 감사 이벤트 발행
- 입력 검증, 중복 충돌 HTTP 409, 미존재 HTTP 404
- 관리자 Basic 인증, 한국어 Admin 브라우저 UI (`http://localhost:8080/`)

## 빠른 실행 (Docker Compose)

**필요 조건:** Docker Engine/Compose v2, 로컬 8080·5432·6379·9092 포트, 최초 빌드를 위한 인터넷.

```bash
cp .env.example .env
# .env 의 ADMIN_PASSWORD 와 POSTGRES_PASSWORD 를 안전한 값으로 반드시 변경
# ADMIN_PASSWORD는 12글자 이상

docker compose up --build -d
docker compose logs -f app
```

브라우저에서 http://localhost:8080/ 을 열고 `.env`에 설정한 `ADMIN_USERNAME` / `ADMIN_PASSWORD`로 로그인합니다.
기동 확인: `curl http://localhost:8080/actuator/health` (Spring Boot 앱은 초기 DB/Flyway 처리에 수십 초가 걸릴 수 있음).

```bash
# API smoke test (create → get → suspend → delete → 404)
set -a; source .env; set +a
./scripts/smoke-test.sh

# 종료
docker compose down
# 데이터도 삭제하려면: docker compose down -v
```

### 서버만 로컬 실행

Docker는 DB·Redis·Kafka에만 사용하고 애플리케이션은 Java 21에서 개발할 수도 있습니다.

```bash
cp .env.example .env
# 계정/비밀번호 수정
set -a; source .env; set +a
export DB_PASSWORD="$POSTGRES_PASSWORD"
docker compose up -d postgres redis kafka
./gradlew bootRun
```

`gradlew` / `gradlew.bat`는 네트워크가 사용 가능한 컴퓨터에서 Gradle 8.14.3 배포본을 받아 실행하는 **간이 bootstrap 실행기**입니다. 공식 Gradle Wrapper JAR은 이 아카이브에 포함되지 않습니다. 필요하면 설치된 Gradle에서 `gradle wrapper --gradle-version 8.14.3`을 실행해 정식 Wrapper로 교체하세요.

### 테스트

```bash
./gradlew clean test
./gradlew bootJar
```

테스트는 MockK 중심의 단위 테스트로 실제 Docker 컨테이너 없이 실행됩니다. DB/Redis/Kafka와 연결하는 통합 테스트는 후속 작업입니다.

## REST API

인증: 모든 `/api/admin/**` 요청에 `Authorization: Basic ...` 필수. 페이지 인덱스는 0부터 시작하고 `size`는 1~100입니다.

| Method | Path | 설명 |
| --- | --- | --- |
| `GET` | `/api/admin/accounts?keyword=&role=&status=&page=0&size=20` | 검색/정렬(`createdAt DESC`, `id DESC`)/페이지 조회 |
| `GET` | `/api/admin/accounts/{id}` | 상세 조회 (Redis 캐시) |
| `POST` | `/api/admin/accounts` | 생성 (`201 Created`) |
| `PUT` | `/api/admin/accounts/{id}` | 이메일/표시 이름 변경 |
| `PATCH` | `/api/admin/accounts/{id}/role` | `USER` / `ADMIN` 변경 |
| `PATCH` | `/api/admin/accounts/{id}/status` | `ACTIVE` / `SUSPENDED` 변경 |
| `DELETE` | `/api/admin/accounts/{id}` | 소프트 삭제 (`204 No Content`) |

`status` 생략 시 `DELETED` 제외; `status=DELETED`로 명시하면 삭제 계정도 목록 조회 가능하나 상세 조회는 404입니다.

```bash
set -a; source .env; set +a

# Create
curl -u "$ADMIN_USERNAME:$ADMIN_PASSWORD" \
  -H 'Content-Type: application/json' \
  -d '{"loginId":"alice","email":"alice@example.com","displayName":"Alice","role":"USER"}' \
  http://localhost:8080/api/admin/accounts

# Search
curl -u "$ADMIN_USERNAME:$ADMIN_PASSWORD" \
  'http://localhost:8080/api/admin/accounts?keyword=alice&page=0&size=20'
```

응답 예시:

```json
{
  "items": [{"id":"00000000-0000-0000-0000-000000000001","loginId":"alice","email":"alice@example.com","displayName":"Alice","role":"USER","status":"ACTIVE","createdAt":"2026-10-15T01:00:00Z","updatedAt":"2026-10-15T01:00:00Z"}],
  "page":0,"size":20,"totalElements":1,"totalPages":1
}
```

전체 API 계약: [`specs/001-account-admin/contracts/openapi.yaml`](specs/001-account-admin/contracts/openapi.yaml)

## Spec Kit + GitHub Copilot 작업 흐름

ZIP에는 **명세 문서가 미리 작성되어 있지만**, 공식 Spec Kit CLI가 설치하는 관리 스크립트/명령 파일은 미포함입니다. 먼저 공식 CLI로 초기화해 Copilot 통합 명령을 설치하세요. `--force` 적용 전 파일 변경 사항은 Git에서 관리하는 것을 권장합니다.

```bash
# 공식 문서: https://github.com/github/spec-kit
uv tool install specify-cli
specify init --here --force --integration copilot
# VS Code 또는 GitHub Copilot CLI에서 현재 폴더 열기
```

Copilot Chat에서 다음 순서로 기존 명세를 검토하고 기능을 확장합니다. 버전에 따라 슬래시 명령 구분자(`.` / `-`)가 다를 수 있으니 설치한 CLI의 도움말을 우선합니다.

1. `/speckit.constitution` — `.specify/memory/constitution.md` 기반 원칙 검토
2. `/speckit.specify` — `specs/001-account-admin/spec.md`의 요구사항 검토·변경
3. `/speckit.plan` — 기술 설계 및 `plan.md`, `research.md`, `data-model.md` 갱신
4. `/speckit.tasks` — `tasks.md` 갱신
5. `/speckit.implement` — 작업별 구현/테스트
6. `/speckit.converge` — 명세·구현 간 누락 검토

Copilot 프로젝트 지침은 `.github/copilot-instructions.md`에 포함했습니다. 명세를 수정할 때 API 계약서와 테스트도 함께 갱신하세요.

## 프로젝트 구조

```text
account-admin-spec-kit/
├── .github/copilot-instructions.md
├── .specify/memory/constitution.md
├── specs/001-account-admin/
│   ├── overview-ko.md         # 한글 스펙 요약 및 문서 안내
│   ├── spec.md                 # 목적/사용자 시나리오/인수 조건
│   ├── plan.md                 # 구조와 설계 선택
│   ├── tasks.md                # 작업 체크리스트
│   ├── research.md             # 선택 배경과 제한
│   ├── data-model.md           # 테이블/상태 모델
│   ├── quickstart.md           # 수동 검증 절차
│   └── contracts/openapi.yaml # API 계약
├── src/main/kotlin/com/example/accountadmin/
│   ├── api/                    # Controller/DTO/Exception handler
│   ├── config/                 # Security, Cache, Kafka topic
│   ├── domain/                 # JPA Entity
│   ├── repository/             # JPA + Querydsl
│   └── service/                # 서비스 + 감사 이벤트 publisher
├── src/main/resources/{db/migration,static}/
├── src/test/kotlin/
├── Dockerfile
├── compose.yaml
└── scripts/smoke-test.sh
```

## 설계 제약 및 운영 전환 체크

- **보안:** 이 프로젝트는 로컬 개발/MVP용입니다. Bootstrap Basic 인증과 CSRF 비활성화는 운영용이 아닙니다. 운영에서는 HTTPS, IdP/OIDC/SSO, 실제 관리자 권한 인가, CSRF 전략, Rate limiting, 로그인 감사, 비밀번호/시크릿 관리를 별도로 적용해야 합니다. UI의 '로그아웃'은 JS 메모리만 초기화하며 브라우저 자체 Basic 인증 캐시는 지우지 못할 수 있습니다.
- **권한 모델:** `AccountRole.ADMIN`은 관리 대상 메타데이터입니다. 관리자 API 접근 권한과 연동하지 않습니다. 계정 비밀번호 저장/인증, 비밀번호 재설정, 사용자 로그인은 포함하지 않습니다.
- **이벤트 보장:** Kafka 발행은 DB commit **후 비동기 best-effort** 방식이므로 Kafka 장애 시 이벤트가 누락될 수 있습니다. 프로덕션에서는 transactional outbox + 재시도/DLQ 필요합니다.
- **캐시:** Redis TTL 5분; 캐시 삭제 실패/Redis 장애 시 오류가 발생할 수 있으며 fallback 정책은 미구현입니다.
- **소프트 삭제:** 중복 재가입 방지를 위해 login_id/email의 UNIQUE를 유지합니다. 개인정보 삭제/복구 정책은 별도 합의가 필요합니다.
- **데이터 모델:** 감사 이벤트는 Kafka로만 전송됩니다. DB 감사 기록 저장, 계정 운영자별 세분화 권한, 사용자 대량 가져오기 기능은 범위 밖입니다.
- **검증:** ZIP은 코드/설계 정적 검사를 기반으로 생성됐습니다. 제공 환경에 Gradle 배포본·Docker 실행기가 없고 네트워크 다운로드가 불가하여 실제 컴파일/컨테이너 구동 검증은 수행하지 못했습니다.

## 참고

- [GitHub Spec Kit 공식 문서](https://github.github.com/spec-kit/)
- [Spec Kit Copilot 통합 명령](https://github.com/github/spec-kit/blob/main/docs/reference/core.md)
- [Apache Kafka 공식 Docker Compose 예시](https://github.com/apache/kafka/tree/trunk/docker/examples)
