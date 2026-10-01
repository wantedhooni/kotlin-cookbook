# Kotlin Spring Quartz Starter

Spring Boot에서 Quartz 스케줄러를 설정하고 PostgreSQL 기반 JDBC Job Store를 사용하는 Kotlin 예제입니다. 현재 소스는 Spring Boot 애플리케이션 시작점 중심이며, 실제 Quartz Job/Trigger 구현은 포함하지 않습니다.

## 기술 스택

- Kotlin 2.2.21, Java 21, Spring Boot 4.0.7
- Spring Boot Quartz, Spring Data JPA, QueryDSL Jakarta
- PostgreSQL 17

## 실행

JDK 21과 Docker가 필요합니다.

```bash
docker compose up -d postgres
./gradlew bootRun
```

PostgreSQL은 `localhost:5432`, 데이터베이스/사용자는 `app`, 비밀번호는 `app`입니다. 설정에서 Quartz JDBC Store와 스키마 자동 초기화를 활성화하고 있으므로 개발용 데이터베이스에서만 사용하세요. Hibernate DDL 모드도 `create-drop`으로 설정되어 있어 종료 시 JPA 스키마가 삭제됩니다.

테스트:

```bash
./gradlew test
```

스케줄 실행을 확인하려면 Quartz `Job` 구현과 `JobDetail`/`Trigger` 등록을 추가해야 합니다. 현재 앱이 예약 작업을 수행한다고 가정하지 마세요.
