# Kotlin Code Test

Kotlin 표준 라이브러리와 Java 시간 API의 동작을 짧은 실행 파일로 확인하는 실험용 프로젝트입니다. 현재는 Kotlin 컬렉션 타입과 `java.time.Instant`를 출력·필터링하는 예제가 포함되어 있습니다.

## 기술 스택

- Kotlin 2.3.21, Java 21
- Kotlin Test / JUnit 5
- ktlint Gradle 플러그인

## 실행

JDK 21이 필요합니다. Gradle Wrapper로 테스트와 정적 검사 작업을 실행할 수 있습니다.

```bash
./gradlew test
./gradlew ktlintCheck
```

`src/main`의 개별 `main` 함수는 IDE에서 실행할 수 있습니다. `CollectionPrectice.kt`와 `InstantTest.kt`를 각각 실행해 보세요. 현재 클래스 이름의 `Test` 접미사에도 불구하고 `InstantTest`는 JUnit 테스트가 아니라 실행 예제이며, 별도 테스트 케이스가 추가되기 전까지 `test` 작업은 이를 자동 테스트로 실행하지 않습니다.
