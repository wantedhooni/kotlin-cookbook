# 구현 작업 목록: 계정 관리 Admin

**표시 규칙:** `[x]`는 **소스/문서 작성됨**을 뜻하며 테스트 실행 성공을 의미하지 않는다. `[ ]`는 후속 검증·구현 작업이다.

## 1단계. 프로젝트 기반

- [x] **T001** Kotlin/JDK 21, Spring Boot, Querydsl kapt, Kotest/MockK 의존성 설정
- [x] **T002** Flyway 초기 마이그레이션 및 JPA 계정 엔티티 작성
- [x] **T003** Docker Compose, Dockerfile, `.env.example` 작성

## 2단계. 계정 관리 기능

- [x] **T004** JpaRepository 및 Querydsl 동적 검색 작성
- [x] **T005** 입력 검증 DTO, REST 컨트롤러, API 예외 응답 매핑 작성
- [x] **T006** 생성/상세/수정/역할/상태/논리 삭제 서비스 작성

## 3단계. 인프라

- [x] **T007** Redis 상세 캐시 TTL 및 데이터 변경 시 무효화 설정
- [x] **T008** 트랜잭션 커밋 이후 Kafka 감사 이벤트 발행 및 토픽 설정
- [x] **T009** 개발용 HTTP Basic 관리자 인증 설정

## 4단계. 화면·테스트·문서

- [x] **T010** 정적 Admin 화면(로그인·검색·편집 등) 작성
- [x] **T011** Kotest `BehaviorSpec` + MockK 단위 테스트 작성
- [x] **T012** 실행 명령, 보안 제약, API 및 스모크 테스트 문서화
- [ ] **T013** 의존성 접근 가능한 환경에서 `./gradlew test` 실행 및 오류 수정
- [ ] **T014** Docker Compose 기동 후 `scripts/smoke-test.sh` 종단 간 검증
- [ ] **T015** DB/Redis/Kafka Testcontainers 통합 테스트 추가
- [ ] **T016** 운영 전 OIDC/SSO·CSRF·시크릿 관리·권한 인가·Outbox·관측성 강화

## 5단계. Spring Boot 4 마이그레이션

- [x] **T017** Spring Boot 4.1.1, Java 21, Kotlin 2.3.20 버전 적용
- [x] **T018** Boot 4 모듈형 Web MVC/Kafka/Flyway Starter 및 테스트 Starter 적용
- [x] **T019** Jackson 3 Kotlin 모듈 및 Kafka `JacksonJsonSerializer` 전환
- [x] **T020** OpenFeign Querydsl 7.1 Jakarta JPA + kapt 설정으로 전환
- [x] **T021** Kotest/MockK·테스트 의존성·README·Copilot 지침 갱신
- [ ] **T022** `./gradlew clean test bootJar` 및 Compose 스모크 테스트 실행

## 권장 작업 순서

`T013 → T022 → T014 → T015 → T016` 순서로 실제 동작을 확인하고 운영 전 요구사항을 보강한다.
