# 구현 계획서: 계정 관리 Admin

- **관련 명세:** [`spec.md`](spec.md)
- **현재 상태:** 초기 구현 소스 준비 완료, Gradle 빌드 및 Docker Compose 실행 검증 대기

## 1. 기술 구성

| 영역 | 선택 기술 | 역할 |
| --- | --- | --- |
| JVM | Java 21, Kotlin 2.3.20, Spring Boot 4.1.1 | 서버 런타임 |
| 빌드 | Gradle 8.14.3 | 의존성·패키징 |
| REST API | Spring Web MVC, Bean Validation | 요청 처리·검증 |
| 인증 | Spring Security HTTP Basic | 개발용 초기 관리자 인증 |
| RDBMS | PostgreSQL 17, Flyway | 계정 원본 데이터·마이그레이션 |
| ORM/조회 | JPA(Hibernate), OpenFeign Querydsl 7.1, kapt | 영속성·동적 검색 |
| 캐시 | Spring Cache + Redis | 상세 조회 5분 캐싱 |
| 이벤트 | Spring Kafka | 트랜잭션 커밋 이후 감사 이벤트 전송 |
| 테스트 | Kotest BehaviorSpec, MockK | BDD 단위 테스트 |
| 화면 | 정적 HTML/CSS/JavaScript | 최소 기능 관리 콘솔 |

## 2. 전체 아키텍처

```text
관리자 브라우저 (HTML / CSS / JavaScript)
    │ HTTP Basic + REST API
    ▼
/api/admin/accounts
    │
    ▼
AccountAdminController ──▶ AccountService
                                  ├──▶ AccountRepository (JPA + Querydsl) ──▶ PostgreSQL
                                  ├──▶ @Cacheable / @CacheEvict ───────────▶ Redis
                                  └──▶ ApplicationEventPublisher
                                             │ AFTER_COMMIT
                                             ▼
                                      AccountAuditPublisher ───────────────▶ Kafka
                                                                      accounts.audit.v1
```

**계층별 책임**
- `api/`: HTTP 경로·입력 검증·응답 DTO·오류 매핑
- `service/`: 계정 변경 규칙, 트랜잭션, 캐시·감사 이벤트 연결
- `repository/`: JPA 영속성과 Querydsl 검색
- `domain/`: 계정 엔티티 및 역할·상태
- `config/`: 보안, Redis, Kafka 설정

## 3. 주요 설계 결정

1. **관리자 인증 분리:** 관리 대상 `accounts.role=ADMIN`은 실제 관리자 로그인 계정이 아니다. 별도 환경변수의 개발용 관리자를 사용한다.
2. **화면 단순화:** React 등의 도구 없이 정적 페이지를 사용해 초기 의존성과 빌드 단계를 최소화한다.
3. **정규화·중복 제어:** 로그인 ID와 이메일을 저장 시 소문자로 정규화하고 PostgreSQL UNIQUE 제약으로 경합을 차단한다.
4. **논리 삭제:** 삭제 후에도 로그인 ID·이메일을 재사용하지 않는다. 복구 및 재가입 정책은 이 범위에 포함하지 않는다.
5. **캐시 전략:** 검색 목록은 캐싱하지 않고 UUID 기반 상세 조회만 캐싱한다. 계정 변경 시 캐시를 무효화한다.
6. **이벤트 전송:** `AFTER_COMMIT` 리스너로 롤백 트랜잭션의 이벤트 생성을 방지한다. 다만 발행 실패로 이벤트가 누락될 수 있으므로 운영에서는 Outbox를 권장한다.

## 4. 구현 작업 묶음

1. 프로젝트 구조·의존성·Flyway 초기 스키마
2. `Account` 모델 및 Querydsl 기반 동적 검색 저장소
3. 생성·조회·수정·역할/상태 변경·삭제 API와 표준 오류 응답
4. Redis 캐시 및 Kafka 커밋 후 감사 이벤트
5. 개발용 관리자 인증 및 정적 Admin 화면
6. Kotest/MockK 테스트·Docker Compose·README·스모크 테스트

세부 진행 상태는 [`tasks.md`](tasks.md)를 따른다.

## 5. 검증 계획

| 분류 | 실행 방법 | 검증 포인트 | 상태 |
| --- | --- | --- | --- |
| 단위 테스트 | `./gradlew test` | 서비스 정상/예외 분기 | 미실행 |
| 패키징 | `./gradlew bootJar` | Boot 4 의존성·컴파일·Q 타입 생성 | 미실행 |
| 통합 구동 | `docker compose up --build -d` | DB·Redis·Kafka·앱 연결 | 미실행 |
| 스모크 테스트 | `./scripts/smoke-test.sh` | API 생성·조회·상태 변경·삭제 | 미실행 |
| 보안 | 인증 없이 `/api/admin/**` 요청 | HTTP `401` 확인 | 미실행 |

## 6. 위험 및 제약

- 생성 환경에서 외부 의존성을 다운로드할 수 없어 실제 컴파일 결과를 확인하지 않았다.
- HTTP Basic 로그아웃은 브라우저 인증 캐시까지 제거하지 못할 수 있으며 운영용 SSO와 다르다.
- Redis 장애 시 캐시 읽기/무효화 실패가 요청에 영향을 줄 수 있다.
- Transactional Outbox가 없어 Kafka 감사 이벤트의 전달을 보장할 수 없다.
