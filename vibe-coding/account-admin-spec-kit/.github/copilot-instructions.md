# GitHub Copilot 프로젝트 지침: 계정 관리 Admin

## 1. 기술 스택·코딩 규칙

- Java 21, Kotlin 2.3.20, Spring Boot 4.1.1을 사용한다. Kotlin 관용 표현을 따르고 요청/응답 DTO는 가능한 불변 객체로 설계한다.
- PostgreSQL과 Flyway를 사용하며 Hibernate 스키마 자동 갱신은 금지한다(`ddl-auto: validate`).
- JPA 엔티티는 `domain/`, 업무 흐름은 `service/`, 동적 쿼리는 `repository/`, REST API는 `api/`에 둔다.
- 동적 검색과 페이징에는 Querydsl을 사용하고 정렬 순서를 결정적으로 유지한다.
- Redis에는 UUID 기반 계정 상세만 캐싱한다. 계정 변경 시 해당 캐시를 무효화한다.
- Kafka 감사 이벤트는 `ApplicationEventPublisher`와 `@TransactionalEventListener(AFTER_COMMIT)`를 통해 발행한다.
- 관리 대상 계정의 `role=ADMIN`과 실제 관리자 인증 계정은 별개다. `ADMIN` 역할만으로 API 접근을 허용하지 않는다.

## 2. Spec Kit 중심 변경 순서

1. 코드 변경 전에 `.specify/memory/constitution.md`와 `specs/001-account-admin/spec.md`를 읽는다.
2. 기능 변경 시 `spec.md`의 인수 조건과 `contracts/openapi.yaml`을 **먼저 갱신**한다.
3. 구현 전에 `plan.md`, `tasks.md`를 갱신하고 작은 작업 단위로 진행한다.
4. 변경한 업무 규칙마다 Kotest `BehaviorSpec` + MockK 테스트를 추가한다.
5. Java 21에서 `./gradlew test`, `./gradlew bootJar`를 실행한다. 서비스를 시작한 후 `scripts/smoke-test.sh`로 확인한다.
6. 인증 정보는 코드에 직접 넣지 않는다. 로컬은 `.env`, 운영은 별도 시크릿 관리 체계를 사용한다.
7. 인증되지 않은 API 응답으로 계정 정보가 노출되지 않게 한다. 사용자가 입력한 문자열을 HTML로 렌더링하지 않는다.

## 3. 완료 정의(Definition of Done)

- 인수 조건과 OpenAPI 계약이 최신 상태다.
- 입력 검증과 HTTP 오류 응답이 일관적이다.
- Flyway 마이그레이션이 호환되며 API 변경 동작에 대응하는 테스트가 있다.
- 쓰기 경로에서 캐시 무효화와 감사 이벤트 발행을 검증한다.
- README와 운영 절차가 동작과 일치한다. 인프라 이용이 가능하면 수동 스모크 테스트도 수행한다.
- 실행하지 못한 검증을 완료로 표시하지 않는다.

## 4. Spring Boot 4.1.1 호환성

- 모듈형 Starter인 `spring-boot-starter-webmvc`, `spring-boot-starter-kafka`, `spring-boot-starter-flyway` 등을 우선 사용한다. 테스트는 Spring Boot 4 모듈형 Test Starter를 이용한다.
- Jackson 2의 `com.fasterxml.jackson.databind` 대신 Jackson 3의 `tools.jackson.*`를 사용한다. Kafka Producer에는 `JacksonJsonSerializer`를 사용한다.
- Querydsl은 `io.github.openfeign.querydsl:7.1` 계열과 kapt의 JPA APT 분류자를 사용하고, 생성된 `QAccount`가 Kotlin 컴파일에 포함되는지 확인한다.
- Kotest 6.1.x와 JUnit Platform을 연동하고 가짜 의존성은 MockK로 대체한다.
- 과거 Spring Boot 3 예제 코드를 그대로 복사하지 말고 Spring Framework 7·Security 7·최신 Spring Data·공식 마이그레이션 문서와 대조한다.

## 5. 사용자 요청을 처리할 때

- 기술 이름, Java/Kotlin 식별자, API JSON 키·경로·상태 enum은 영문 그대로 유지한다.
- Spec Kit 문서의 기능 설명, 인수 조건, 계획, 작업 항목과 테스트 설명은 **한국어**로 작성한다.
- 사용자에게 완료라고 보고하기 전에 실제 수행한 검증 항목과 미실행 항목을 구분한다.
