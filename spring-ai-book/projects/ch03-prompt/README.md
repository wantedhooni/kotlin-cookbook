# Chapter 03: Prompt

Spring AI에서 프롬프트를 구성하는 여러 방법을 살펴보는 Kotlin/Spring Boot 예제입니다. 현재 디렉터리에는 zero-shot, few-shot, role assignment, multi-message, prompt template, step-back, self-consistency, chain-of-thought 관련 화면 템플릿과 정적 리소스가 있습니다. 이 체크아웃에는 Kotlin/Java 애플리케이션 소스가 포함되어 있지 않으므로 실행 가능한 controller나 prompt 처리 흐름은 제공하지 않습니다.

## 기술 스택

- Kotlin 2.3.21, Java 21, Spring Boot 3.4.6
- Spring AI 1.0.0 OpenAI model starter
- Spring MVC, WebFlux, Thymeleaf

## 설정

`src/main/resources/application.properties`에서 OpenAI API 키를 환경변수로 받습니다.

```bash
export OPENAI_API_KEY='...'
```

실제 키를 저장소에 커밋하지 마세요. 기본 포트 설정은 `8080`입니다.

## 빌드

```bash
./gradlew test
```

빌드 설정 및 화면 템플릿을 확인하는 용도로 사용할 수 있습니다. Spring Boot 애플리케이션을 실행하려면 먼저 애플리케이션 진입점과 필요한 controller/service 코드를 추가해야 합니다.
