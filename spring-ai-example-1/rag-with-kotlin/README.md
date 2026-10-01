# Kotlin RAG Dog Adoption Demo

Spring AI와 pgvector를 이용해 강아지 입양 정보를 검색하고 질문에 답하는 Kotlin 예제입니다. 애플리케이션 시작 시 `dog` 테이블의 레코드를 vector store에 문서로 추가하고, `QuestionAnswerAdvisor`를 적용한 OpenAI ChatClient로 검색 증강 응답을 생성합니다. `GET /dogs`는 저장된 강아지 목록을 반환합니다.

## 기술 스택

- Kotlin 2.3.21, Java 21 (Gradle 설정 기준)
- Spring Boot 4.1.1, Spring AI 2.0.0
- OpenAI Chat Model, Spring AI pgvector Vector Store
- PostgreSQL/pgvector, Spring JDBC, Spring MVC

> 프로젝트에는 Gradle과 Maven 빌드 파일이 모두 있습니다. 두 빌드 파일의 Spring Boot/Kotlin 버전은 서로 다르므로 아래 실행 예시는 프로젝트의 Gradle Wrapper를 기준으로 합니다.

## 사전 준비 및 실행

JDK 21, Docker, OpenAI API 키가 필요합니다. OpenAI 키는 `src/main/resources/application.properties`의 예시값 대신 환경변수로 설정하세요.

```bash
export OPENAI_API_KEY='...'
./gradlew bootRun
```

Spring Boot Docker Compose 연동이 `compose.yaml`의 pgvector PostgreSQL을 시작하고 종료합니다. 데이터베이스는 `mydatabase`, 사용자 `myuser`, 비밀번호 `secret`, 포트 `5432`로 설정되어 있습니다. 애플리케이션은 기본 포트 `8090`에서 실행됩니다. 첫 실행 시 pgvector schema 초기화와 `schema.sql`/`data.sql` 초기 데이터 설정을 수행합니다.

목록 API:

```bash
curl http://localhost:8090/dogs
```

테스트:

```bash
./gradlew test
```

테스트 설정은 H2를 사용하고 Docker Compose 시작과 SQL 초기화를 비활성화합니다. 실제 ChatClient 응답을 확인하려면 유효한 OpenAI API 키와 실행 가능한 pgvector 서비스를 사용해야 합니다. 이 프로젝트는 학습 예제이며, 시작 시 기본 질문으로 RAG 응답을 생성합니다.
