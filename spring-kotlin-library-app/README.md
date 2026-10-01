# Spring Kotlin Library App

Kotlin과 Spring Boot로 도서관 도메인을 구성한 예제입니다. 책과 사용자, 대출 이력을 JPA Entity로 모델링하고, 사용자 등록·조회·수정·삭제와 대출 이력 조회를 Service 계층에서 다룹니다. QueryDSL 설정과 H2 기반 로컬 개발 구성이 포함되어 있습니다.

## 기술 스택

- Kotlin 2.4.20, Java 21, Spring Boot 3.3.13
- Spring Web, Spring Data JPA
- QueryDSL JPA 5.1 (Jakarta), H2

## 실행

JDK 21이 필요합니다. 인메모리 H2를 사용하므로 별도 데이터베이스 컨테이너 없이 시작할 수 있습니다.

```bash
./gradlew bootRun
```

테스트:

```bash
./gradlew test
```

H2 콘솔은 `/h2-console`에 활성화되어 있으며 JDBC URL은 `jdbc:h2:mem:library`, 사용자명은 `user`입니다. 애플리케이션 설정에서 Hibernate가 개발용 스키마를 생성합니다.

현재 구현된 도메인/서비스와 실제 노출된 HTTP API는 구분해서 확인하세요. 저장소에 Controller가 없는 상태라면 서비스 메서드는 REST endpoint로 직접 호출할 수 없습니다.
