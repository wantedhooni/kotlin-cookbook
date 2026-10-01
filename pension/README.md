# Pension Service Domain

연금 계좌와 납입금, 투자 지시·주문·체결, 보유 자산 및 원장 이벤트의 도메인 모델을 다루는 Kotlin/Spring Boot 프로젝트입니다. 계좌 잔고 예약, 매수 정산, 낙관적 잠금, 멱등성/Outbox 모델 등 금융 도메인의 기본 구조를 실험합니다.

## 기술 스택

- Kotlin 2.3.21, Java 21, Spring Boot 4.1.1
- Spring Web, Spring Data JPA, Validation, Actuator
- PostgreSQL 18.6, Flyway, Redisson
- JUnit 및 PostgreSQL/Redis Testcontainers

## 실행

JDK 21과 Docker가 필요합니다. 로컬 데이터베이스와 Redis를 시작합니다.

```bash
docker compose up -d
./gradlew bootRun
```

기본 포트는 애플리케이션 `8080`, PostgreSQL `5432`, Redis `6379`입니다. 애플리케이션 설정은 `src/main/resources/application.yml`에 있으며, DB 접속 정보는 `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`로, Redis는 `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`로 재정의할 수 있습니다. Flyway가 DB migration을 적용합니다.

테스트:

```bash
./gradlew test
```

현재 저장소에는 도메인과 인프라 기반이 중심으로 들어 있으며, 완성된 연금 상품 API나 외부 금융기관 연동을 제공하는 프로젝트는 아닙니다. Redis는 Compose에 포함되어 있지만 애플리케이션의 Redisson 설정과 연결을 확인한 뒤 사용하세요.
