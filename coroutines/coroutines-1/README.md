# Coroutine JPA Lab

Kotlin 코루틴의 `suspend` 함수가 Spring MVC 요청을 처리할 때 어떤 차이를 만드는지 비교하는 Spring Boot 실습 프로젝트입니다. PostgreSQL/JPA 계좌 CRUD 예제도 함께 두어 blocking persistence와 coroutine 기반 endpoint를 함께 살펴볼 수 있습니다.

## 기술 스택

- Kotlin 2.3.21, Java 21
- Spring Boot 4.1.1, Spring MVC, Spring Data JPA
- `kotlinx-coroutines-core`, `kotlinx-coroutines-reactor`
- PostgreSQL 17

## 예제 API

| Method | 경로 | 동작 |
| --- | --- | --- |
| `GET` | `/labs/coroutines/blocking` | 일반 함수에서 `Thread.sleep(3초)` |
| `GET` | `/labs/coroutines/suspend-blocking` | `suspend` 함수 안에서 `Thread.sleep(3초)` |
| `GET` | `/labs/coroutines/suspend-delay` | `suspend` 함수 안에서 non-blocking `delay(3초)` |
| `POST` | `/accounts` | 계좌 생성 |
| `GET` | `/accounts/{id}` | ID로 계좌 조회 |
| `GET` | `/accounts` | 전체 계좌 조회 |

`suspend` 선언만으로 blocking 호출이 non-blocking으로 바뀌지 않습니다. 첫 두 coroutine 실습 endpoint는 요청 스레드를 점유하고, `delay` 예제는 대기 중 코루틴을 중단합니다. 동시 요청의 응답 시간과 로그를 비교해 보세요.

계좌 생성 예:

```bash
curl -X POST http://localhost:8080/accounts \
  -H 'Content-Type: application/json' \
  -d '{"owner":"Kim","balance":1000.00}'
```

## 실행

JDK 21과 Docker가 필요합니다. PostgreSQL 컨테이너와 애플리케이션은 별도로 실행합니다.

```bash
docker compose up -d postgres
./gradlew bootRun
```

PostgreSQL 기본 접속 정보는 `app/app`이며 DB 이름도 `app`입니다. 로컬 애플리케이션은 `localhost:5432`를 사용합니다. JPA 스키마는 개발 설정에서 `create-drop`으로 생성되므로 애플리케이션 재시작 시 데이터가 초기화됩니다.

테스트:

```bash
./gradlew test
```
