# Stock Exchange Starter

주식 거래 도메인 프로젝트를 위한 Kotlin/Spring Boot 시작점입니다. 현재 구현은 애플리케이션 bootstrap과 데이터베이스 설정 중심이며, 주문·체결 API나 거래 도메인 로직은 아직 포함하지 않습니다.

## 기술 스택

- Kotlin 2.2.21, Java 21, Spring Boot 4.0.8
- Spring MVC, Spring Data JPA, Jakarta Validation
- PostgreSQL 17, Flyway
- JUnit 및 Testcontainers

## 실행

JDK 21과 Docker가 필요합니다.

```bash
docker compose up -d postgres
./gradlew bootRun
```

PostgreSQL 기본 설정은 `app/app`, 데이터베이스 `app`, 포트 `5432`입니다. Spring 설정은 `src/main/resources/application.yml`에서 확인할 수 있습니다. Flyway가 활성화되어 있지만 현재 저장소에는 migration 파일이 없습니다. 실제 테이블이 필요한 도메인을 추가할 때 migration도 함께 작성해야 합니다.

테스트:

```bash
./gradlew test
```

거래소 동작을 테스트하는 API가 필요한 경우 도메인, migration, 서비스 및 controller를 추가해야 합니다. 현재 프로젝트는 실제 거래소 연동이나 주문 매칭을 수행하지 않습니다.
