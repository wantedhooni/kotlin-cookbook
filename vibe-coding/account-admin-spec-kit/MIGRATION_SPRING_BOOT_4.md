# Spring Boot 4.1.1 마이그레이션 정리

## 1. 변경 범위

기존 Spring Boot 3.5.7 기반 계정 관리 프로젝트를 **Spring Boot 4.1.1**로 변경했다. 기존 REST 엔드포인트, PostgreSQL 계정 테이블 구조, Redis 캐시 이름, Kafka `accounts.audit.v1` 토픽은 유지한다. 프레임워크 버전 변경 자체를 위한 DB 마이그레이션은 필요하지 않다.

## 2. 기술·의존성 변경

| 구성 요소 | 적용 내용 |
| --- | --- |
| JVM/언어 | Java 21, Kotlin 2.3.20 (`jvm`, `plugin.spring`, `plugin.jpa`, `kapt`) |
| 빌드 | Gradle 8.14.3 |
| 프레임워크 | Spring Boot 4.1.1 / Spring Framework 7 / Jakarta EE 11 |
| Web MVC | `spring-boot-starter-web`에서 `spring-boot-starter-webmvc`로 전환 |
| Kafka | 직접 `spring-kafka` 의존성 대신 `spring-boot-starter-kafka` 사용 |
| Flyway | `spring-boot-starter-flyway` + `flyway-database-postgresql` |
| JSON | Jackson 3의 `tools.jackson.module:jackson-module-kotlin` 사용 |
| Kafka JSON | Jackson 3 기반 `JacksonJsonSerializer` 사용 |
| Querydsl | OpenFeign Querydsl 7.1, kapt JPA APT(`:jpa`) 적용 |
| 테스트 | `spring-boot-starter-webmvc-test`, `spring-boot-starter-security-test`, Kotest 6.1.11, MockK 1.14.9 |

## 3. 개발 환경에서 검증하기

```bash
./gradlew clean test bootJar
cp .env.example .env
# ADMIN_PASSWORD는 12자 이상, POSTGRES_PASSWORD도 안전한 값으로 변경

docker compose up --build -d
set -a; source .env; set +a
./scripts/smoke-test.sh
```

## 4. 확인이 필요한 사항

- ZIP 생성 환경에서는 Gradle/Docker 실행 및 외부 의존성 다운로드가 불가하여 실제 컴파일·통합 검증을 수행하지 못했다.
- 아카이브의 Gradle 실행 스크립트는 **공식 Gradle Wrapper JAR이 없는 간이 부트스트랩 실행기**다.
- 관리자 인증은 개발용 HTTP Basic이며, Kafka 감사 이벤트는 커밋 이후 **최선 노력 전송**이다. 운영 제약은 [`README.md`](README.md)를 참조한다.

## 5. 공식 참고 문서

- [Spring Boot 4 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)
- [Spring Boot Reference](https://docs.spring.io/spring-boot/reference/)
- [Spring Data JPA Querydsl](https://docs.spring.io/spring-data/jpa/reference/repositories/core-extensions.html)
- [Spring Kafka Change History](https://docs.spring.io/spring-kafka/reference/appendix/change-history.html)
