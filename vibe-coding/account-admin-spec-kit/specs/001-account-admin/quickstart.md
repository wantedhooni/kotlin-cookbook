# 빠른 실행 및 수동 인수 테스트

## 실행 전 확인

- Docker Engine, Docker Compose v2, Java 21(로컬 Gradle 실행 시)이 필요하다.
- `8080`, `5432`, `6379`, `9092` 포트 사용 가능 여부를 확인한다.
- 초기 이미지/의존성 다운로드를 위한 네트워크 연결이 필요하다.

## 1. 환경변수 및 컨테이너 실행

```bash
cp .env.example .env
# .env의 ADMIN_PASSWORD(최소 12자), POSTGRES_PASSWORD 변경

docker compose up --build -d
docker compose logs -f app
```

## 2. 브라우저에서 인수 조건 확인

1. `http://localhost:8080/`에 접속하여 `.env`의 관리자 정보로 로그인한다.
2. `alice` 계정을 생성하고 기본 역할 `USER`, 상태 `ACTIVE`를 확인한다.
3. `alice`를 검색하고 목록·상세 조회 결과를 확인한다.
4. 이메일과 표시 이름을 변경한 다음 결과를 확인한다.
5. 역할을 `USER → ADMIN → USER`로 변경한다.
6. 상태를 `ACTIVE → SUSPENDED → ACTIVE`로 변경한다.
7. 계정을 삭제하고 기본 목록에서 사라졌는지 확인한다.
8. `status=DELETED` 목록 검색에는 나타나지만 상세 조회는 HTTP `404`인지 확인한다.

## 3. API 스모크 테스트

```bash
set -a; source .env; set +a
./scripts/smoke-test.sh
```

스모크 테스트는 생성 → 상세 조회 → 상태 변경 → 논리 삭제 → 404 흐름을 확인한다.

## 4. Kafka 발행 확인

```bash
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh   --bootstrap-server kafka:19092   --topic accounts.audit.v1   --from-beginning
```

변경 이벤트를 확인한 후 `Ctrl+C`로 소비자를 종료한다.

## 5. Gradle 테스트와 종료

```bash
./gradlew clean test bootJar

docker compose down
# 데이터까지 제거해야 할 때만: docker compose down -v
```

> **검증 상태:** 본 ZIP은 명세 한글화 작업만 반영했다. Gradle 빌드와 Docker 기반 통합 테스트는 해당 실행 환경에서 별도 확인해야 한다.
