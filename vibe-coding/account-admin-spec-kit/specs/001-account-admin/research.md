# 기술 검토 및 대안 선택 기록

## 1. Spec Kit + GitHub Copilot

- 프로젝트 ZIP에는 Spec Kit 명세 문서가 포함되어 있다.
- 공식 CLI가 관리하는 스크립트와 Copilot 명령은 프로젝트 압축 해제 후 별도로 설치한다.
- 초기화 명령: `specify init --here --force --integration copilot`
- CLI 버전에 따라 파일 구성이 다를 수 있으므로 수동으로 관리 파일을 만들어 넣지 않는다.
- 참고: https://github.github.com/spec-kit/

## 2. Kotlin + Querydsl

- OpenFeign Querydsl 7.1(`io.github.openfeign.querydsl`)을 사용한다.
- JPA APT 의존성은 `querydsl-apt:7.1:jpa`, `kapt`로 Q 타입(`QAccount`)을 생성한다.
- Hibernate 프록시 지원을 위해 Kotlin JPA 플러그인 및 all-open 설정을 사용한다.
- 선택 이유: 키워드·역할·상태 조합에 대해 타입 안전 동적 검색을 작성하기 위함이다.

## 3. PostgreSQL + Flyway

- 모든 DB 변경을 Flyway 버전 마이그레이션으로 기록한다.
- Kotlin enum 검증 외에 PostgreSQL CHECK 제약조건으로 잘못된 상태·역할 값을 방어한다.
- JPA는 스키마 검증(`validate`)만 수행한다.

## 4. Kafka 개발 환경

- 단일 노드 KRaft 구성으로 개발용 브로커를 실행한다.
- Compose 내부 접근 주소는 `kafka:19092`, 호스트 접근 주소는 `localhost:9092`이다.
- 운영용 다중 브로커·장애 복구 구성이 아니다.
- 참고: https://github.com/apache/kafka/tree/trunk/docker/examples

## 5. Redis 캐시

- UUID 기반 계정 상세 조회만 캐싱하고 검색 결과는 캐싱하지 않는다.
- 검색 페이지 캐시는 필터 조합과 수정 시 무효화가 복잡하므로 초기 범위에서 제외한다.
- TTL은 5분이다.

## 6. 인증 방식 비교

- **선택:** 환경변수 기반 HTTP Basic(개발용 초기 관리자)
- **보류:** OIDC/SSO, 세분화 권한 체계(RBAC)
- **주의:** `accounts.role=ADMIN`은 API 접근 권한을 부여하지 않는다. 운영 적용 전 별도의 인증·인가 설계가 필요하다.

## 7. 감사 이벤트 전달 보장

- **선택:** `@TransactionalEventListener(AFTER_COMMIT)` + Kafka 비동기 전송
- **장점:** DB 롤백 이후에는 이벤트를 발행하지 않는다.
- **제약:** DB 커밋과 Kafka 전송이 하나의 원자적 트랜잭션은 아니다. 전송 실패 시 누락 가능성이 있다.
- **운영 대안:** Transactional Outbox + 재시도 및 DLQ

## 8. 관리자 웹 화면

- **선택:** 정적 HTML/CSS/JavaScript
- **이유:** 별도의 Node/SPA 빌드 도구 없이 관리 기능의 주요 흐름을 빠르게 검증한다.
- 사용자 입력 문자열은 DOM `textContent`를 이용해 표시하여 HTML 주입을 피한다.

## 9. Spring Boot 4 마이그레이션 검토 (2026-10-15)

- 목표: Spring Boot **4.1.1**, Spring Framework **7**, Jakarta EE **11**, Java **21**, Kotlin **2.3.20**
- Web MVC·Kafka·Flyway 및 테스트용 **모듈형 Starter**로 의존성을 재구성한다.
- JSON: Jackson 2 대신 Jackson 3의 `tools.jackson.*`를 사용하고 Kafka Producer에 `JacksonJsonSerializer`를 적용한다.
- Querydsl: 기존 Querydsl 5 Jakarta 의존성 대신 OpenFeign Querydsl 7.1과 JPA APT(`:jpa`)를 사용한다.
- Kotlin: JPA 플러그인, all-open, 애너테이션 전파 및 kapt 설정을 검토한다.
- **남은 검증:** 외부 저장소 접근 가능한 개발 환경에서 컴파일·단위 테스트·컨테이너 통합 검증을 수행해야 한다.

**참고 자료**
- Spring Boot 4 마이그레이션: https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide
- Spring Data JPA Querydsl: https://docs.spring.io/spring-data/jpa/reference/repositories/core-extensions.html
- Spring Kafka 변경 이력: https://docs.spring.io/spring-kafka/reference/appendix/change-history.html
