# Kotlin Cookbook

Kotlin과 Spring 생태계의 기능을 작은 예제와 도메인 중심 프로젝트로 학습하는 저장소입니다. 코루틴, Spring Batch, Spring AI, WebFlux부터 금융·증권 도메인의 백엔드 예제까지 주제별 프로젝트를 모았습니다.

## 프로젝트 구성

| 경로 | 내용 |
| --- | --- |
| [`coroutines/`](coroutines/) | Kotlin 코루틴의 실행, 취소, 예외 처리와 비동기 작업 조합 |
| [`kotlin_domain/`](kotlin_domain/) | Kotlin으로 도메인 모델과 핵심 비즈니스 규칙 구성 |
| [`webflux/`](webflux/) | Spring WebFlux, R2DBC, 리액티브 데이터 접근 예제 |
| [`spring_batch/`](spring_batch/) | 장별 Spring Batch 학습 예제. Chapter 06은 Quartz 스케줄링과 커스텀 Reader를 다룹니다 |
| [`quartz/`](quartz/) | Quartz 기반 스케줄링 예제 |
| [`finance/`](finance/) | 외환·주식 거래와 PG 정산 등 금융 도메인 예제 |
| [`securities/stock-trading-system/`](securities/stock-trading-system/) | 주문·체결·잔고·계좌·D+2 정산을 다루는 이벤트 기반 주식 거래 시스템 |
| [`pension/`](pension/) | 연금 도메인 애플리케이션 예제 |
| [`spring-ai-example-1/`](spring-ai-example-1/) | Kotlin 기반 Spring AI Hello World, Function Calling, RAG 예제 |
| [`spring-ai-book/`](spring-ai-book/) | Spring AI 학습 자료와 챕터별 샘플 프로젝트 |
| [`book-spring-ai-java/`](book-spring-ai-java/) | Spring AI 책의 챕터별 Java 샘플 프로젝트 |
| [`spring-kotlin-library-app/`](spring-kotlin-library-app/) | Kotlin/Spring 기반 도서관 애플리케이션 |
| [`code-test/`](code-test/) | Kotlin 언어 및 라이브러리 동작을 확인하는 코드 실험 |

### 금융 프로젝트

- [`finance/fx-exchange/`](finance/fx-exchange/) — 외환 거래 도메인 예제
- [`finance/stock-exchange/`](finance/stock-exchange/) — 주식 거래 도메인 예제
- [`finance/payment-gateway/pg-settlement-batch/`](finance/payment-gateway/pg-settlement-batch/) — PG 승인·취소 이벤트를 집계하는 Spring Batch 정산 예제 ([상세 README](finance/payment-gateway/pg-settlement-batch/README.md))
- [`finance/payment-gateway/pg-sattlement-1/`](finance/payment-gateway/pg-sattlement-1/) — PG 정산 실습 프로젝트

각 프로젝트의 구현 범위와 실행 조건은 해당 디렉터리의 README, Gradle 설정, 애플리케이션 설정을 확인하세요.

## 시작하기

저장소 루트는 단일 Gradle 멀티모듈 프로젝트가 아닙니다. 각 예제는 독립적인 Gradle 또는 Maven 프로젝트이며, JDK 버전과 필요한 외부 서비스(PostgreSQL, Redis, Kafka 등)는 프로젝트마다 다를 수 있습니다.

예를 들어 코루틴 예제를 실행하려면:

```bash
cd coroutines/coroutines-2
./gradlew test
```

Spring WebFlux 예제를 실행하려면:

```bash
cd webflux/sample-webflux-1
./gradlew bootRun
```

테스트 또는 애플리케이션 실행 명령은 각 하위 프로젝트의 빌드 설정과 README를 기준으로 실행하세요. 프로젝트가 제공하는 Gradle Wrapper를 사용하는 것을 권장합니다.

## 로컬 인프라

일부 프로젝트는 Docker Compose로 데이터베이스나 메시지 브로커를 실행합니다. 프로젝트 디렉터리에서 제공되는 Compose 파일을 확인한 뒤 필요한 서비스만 실행하세요.

```bash
cd <프로젝트-경로>
docker compose up -d
```

Compose 프로젝트들은 독립적으로 구성되어 있으므로 여러 프로젝트를 동시에 실행하면 기본 포트가 충돌할 수 있습니다. 실행 전에 각 프로젝트의 Compose 파일과 애플리케이션 설정에서 포트를 확인하세요.

## 참고

- 이 저장소의 예제는 학습과 실험을 위한 것이며, 실제 금융·결제 서비스의 규정이나 외부 시스템 연동을 그대로 대체하지 않습니다.
- API 키, 비밀번호 등 민감한 설정은 저장소에 기록하지 말고 각 프로젝트가 제공하는 환경변수 또는 로컬 설정을 사용하세요.
- 하위 프로젝트의 기술 스택과 지원 버전은 서로 다를 수 있습니다.
