# FX Exchange Domain

통화, 통화쌍, 환율, 환전 corridor와 spread 정책, 고객 계좌 및 잔고 원장 모델을 정리하는 Kotlin/Spring Boot 프로젝트입니다. 현재 코드는 도메인 모델과 애플리케이션 시작점 중심의 초기 단계이며, 환전 REST API나 거래 처리 흐름은 아직 포함하지 않습니다.

## 도메인 구성

- `domain/common`: 통화
- `domain/fx`: 통화쌍, 환율, FX corridor, spread tier/policy
- `domain/account`: 계좌, 계좌 보유자, 금융기관, 잔고, 원장 및 증권 포지션

## 기술 스택

- Kotlin 2.2.21, Java 21, Spring Boot 4.0.8
- Spring MVC, Spring Data JPA, Jakarta Validation
- PostgreSQL 17, Flyway
- JUnit 기반 테스트 및 Testcontainers 의존성

## 실행

JDK 21과 Docker가 필요합니다.

```bash
docker compose up -d postgres
./gradlew bootRun
```

Compose는 `app` 데이터베이스와 `app/app` 계정을 `localhost:5432`에 제공합니다. 애플리케이션의 DB 연결 설정은 별도로 구성해야 하며, 예를 들어 실행 시 환경변수로 전달할 수 있습니다.

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/app \
SPRING_DATASOURCE_USERNAME=app \
SPRING_DATASOURCE_PASSWORD=app \
./gradlew bootRun
```

컴파일 및 테스트:

```bash
./gradlew test
```

이 저장소의 금융 도메인 코드는 학습용 모델입니다. 실제 환전 가격, 환율 시세, 수수료, 거래 한도와 결제·원장 처리는 아직 구현되어 있지 않습니다.
