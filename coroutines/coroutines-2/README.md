# Coroutines-2

Kotlin 코루틴을 실습하면서 `suspend`, `launch`, `async`, `withContext`, `Job`, `CoroutineScope`, `Cancellation`, `ExceptionHandler` 등을 직접 다뤄보는 프로젝트입니다.

이 프로젝트는 단순히 코루틴 문법을 암기하는 수준을 넘어서, "왜 코루틴이 필요한지", "스레드보다 왜 가볍고 안전한지", "비동기 코드를 어떻게 순서처럼 읽을 수 있는지"를 체험하는 데 초점을 둡니다.

---

## 프로젝트 목표

코루틴은 기존의 스레드 기반 비동기 처리보다 다음과 같은 장점을 줍니다.

- 스레드 생성 비용이 크지 않음
- 비동기 코드를 동기 코드처럼 깔끔하게 작성 가능
- 중단 가능한 함수(`suspend`)로 제어 흐름을 자연스럽게 표현 가능
- 여러 작업을 병렬로 실행하면서도 결과를 조합하기 쉬움

이 프로젝트는 실습용 예제들을 통해 아래 내용을 학습합니다.

- 코루틴의 기본 실행 구조
- `launch`와 `async`의 차이
- `delay`와 `yield`로 비동기 흐름 제어하기
- 취소와 예외 전파
- `CoroutineScope`, `Dispatchers`, `SupervisorJob`
- `withContext`, `withTimeoutOrNull` 활용
- 비동기 데이터 조회를 동기처럼 조립하는 패턴

---

## 프로젝트 구조

```text
src/main/kotlin/com/example/revy/
├── Main.kt
├── Case03.kt
├── Case04.kt
├── Case05.kt
├── Case06.kt
├── Case07.kt
├── Case08.kt
├── Case09.kt
├── Log.kt
├── UserService.kt
├── UserServiceV2.kt
└── ...
```

핵심 포인트는 아래와 같습니다.

- `Case03.kt`: `launch`를 이용한 동시 실행 예제
- `Case04.kt`: 취소(Cancel)와 `isActive` 기반 처리
- `Case05.kt`: 예외 처리와 `SupervisorJob`
- `Case06.kt`: `yield`를 이용한 협조적 스케줄링
- `Case07.kt`: `CoroutineScope`와 `CoroutineContext` 정리
- `Case08.kt`: `async`, `await`, `withContext`, `withTimeoutOrNull`
- `Case09.kt`: 가장 기본적인 코루틴 구조와 순서 제어
- `UserServiceV2.kt`: 실제 서버처럼 비동기적으로 사용자 정보를 조합하는 예제

---

## 핵심 개념 정리

### 1. suspend 함수

코루틴에서 어떤 함수가 긴 시간이 걸리는 작업을 수행할 수 있다는 것을 표현할 때 `suspend`를 사용합니다.

```kotlin
suspend fun fetchUser(): User {
    delay(1000L)
    return User(1L, "revy")
}
```

`suspend` 함수는 내부적으로 중단 가능한 상태 머신으로 동작합니다. 호출자는 그 함수가 끝날 때까지 기다리되, 스레드를 점유한 채 막아버리는 방식이 아니라 비동기 흐름을 유지합니다.

이 점이 코루틴을 "스레드가 아닌 작업 단위의 흐름"으로 다루게 만드는 핵심입니다.

### 2. launch vs async

코루틴은 작업을 시작하는 방식이 여러 가지입니다.

#### `launch`

결과를 바로 반환하지 않고, 작업을 백그라운드에서 실행합니다.

```kotlin
launch {
    delay(1000L)
    println("작업 완료")
}
```

주로 "실행만 시키고 끝나는 작업"에 적합합니다.

#### `async`

결과를 `Deferred`로 돌려줍니다. 나중에 `await()`로 값을 받습니다.

```kotlin
val total = async {
    val a = apiCall1()
    val b = apiCall2()
    a + b
}

println(total.await())
```

이 패턴은 여러 네트워크 호출을 동시에 시작하고 결과를 조합하는 데 매우 효과적입니다.

---

### 3. delay와 yield

코루틴은 스레드를 직접 멈추는 방식이 아니라, 실행 흐름을 일시 정지하고 다시 이어가도록 설계됩니다.

```kotlin
launch {
    println("START")
    delay(500L)
    println("END")
}
```

`delay`는 현재 코루틴을 잠시 멈추지만 스레드는 다른 작업을 수행할 수 있습니다. 이는 스레드 하나로 여러 코루틴을 처리할 수 있게 만드는 핵심입니다.

`yield`는 작업을 다른 코루틴에게 양보하는 역할을 하며, 협업형 스케줄링의 대표 예입니다.

---

### 4. 취소와 예외 처리

코루틴은 스레드처럼 단순히 "강제 종료"되는 것이 아니라, 취소 신호를 받아 안전하게 정리할 수 있습니다.

```kotlin
val job = launch {
    try {
        delay(1000L)
    } catch (e: CancellationException) {
        println("취소 처리")
    } finally {
        println("리소스 정리")
    }
}

job.cancel()
```

중요한 건 취소가 예외와 연결되어 있다는 점입니다. 예외는 코루틴 트리 전체로 전파되며, `SupervisorJob` 같은 전략을 사용해 일부 자식 작업의 실패가 전체를 죽이지 않도록 제어할 수 있습니다.

---

### 5. CoroutineScope와 Dispatchers

코루틴은 어떤 컨텍스트에서 실행될지 결정해야 합니다.

```kotlin
CoroutineScope(Dispatchers.Default).launch {
    // 백그라운드 스레드에서 실행
}
```

`Dispatchers.Default`는 CPU 집약 작업에 적합하고, `Dispatchers.IO`는 I/O 작업에 적합합니다. 이런 컨텍스트 분리를 통해 작업을 효율적으로 배치할 수 있습니다.

또한 `CoroutineScope`는 코루틴의 생명주기를 관리하는 범위를 제공합니다. 스코프를 취소하면 그 안의 모든 자식 코루틴도 함께 정리됩니다.

---

### 6. withContext와 병렬 처리

여러 네트워크/DB 호출을 동시에 수행하면서 값들을 조합할 때 `async`와 `withContext`가 자주 쓰입니다.

```kotlin
suspend fun calculateResult(): Int = withContext(Dispatchers.Default) {
    val num1 = async { fetchA() }
    val num2 = async { fetchB() }
    num1.await() + num2.await()
}
```

이 패턴은 스레드를 직접 관리하는 것보다 훨씬 안전하고 표현력이 좋습니다. 비동기 로직을 마치 동기 코드처럼 읽기 좋게 만들 수 있습니다.

---

## 실습 예제 흐름

### Example 1: launch 기반 비동기 실행

`Case03.kt`와 `Case06.kt`에서는 `launch`로 두 작업을 동시에 실행시키는 예를 보여줍니다. 스레드를 직접 조작하지 않고도 여러 작업이 동시에 시작되고 각기 다른 타이밍에 끝나는 구조를 볼 수 있습니다.

### Example 2: 취소와 정리

`Case04.kt`는 코루틴을 취소하는 시나리오를 담고 있습니다. `cancel()`을 호출해도 즉시 죽는 것이 아니라, `finally`에서 정리 작업을 수행할 수 있다는 점을 학습합니다.

### Example 3: 예외 처리

`Case05.kt`는 예외가 발생했을 때 코루틴의 동작을 설명합니다. `CoroutineExceptionHandler`, `SupervisorJob`, `try-catch` 등을 통해 "어디서 예외를 잡아야 하는지"를 이해할 수 있습니다.

### Example 4: 비동기 결과 조합

`Case08.kt`는 `async`와 `await`를 조합해 여러 작업의 결과를 합치는 예제를 보여줍니다. 이는 실무에서 API 호출, DB 조회, 캐시 조회 등을 동시에 묶어서 처리하는 대표적인 패턴입니다.

### Example 5: 사용자 조회 서비스

`UserServiceV2.kt`는 사용자 프로필과 이미지를 순차적으로 가져오는 비동기 로직을 구현합니다.

```kotlin
suspend fun findUser(userId: Long): UserDtoV2 {
    val profile = userProfileRepository.findProfile(userId)
    val image = userImageRepository.findImage(profile)
    return UserDtoV2(profile, image)
}
```

이 코드는 비동기 I/O가 많아도 `suspend` 함수 안에서는 읽기 쉬운 흐름을 유지할 수 있음을 보여줍니다. 실제로는 서버 코드에서 DB 조회, 이미지 서비스 호출, 사용자 정보 조립 등이 이처럼 자연스럽게 표현됩니다.

---

## 왜 코루틴이 중요한가?

코루틴은 단지 "비동기 코드의 문법 편의성"을 넘어서, 현대 서버 개발의 핵심 도구입니다.

- Spring WebFlux, Ktor, Reactor와 함께 사용 가능
- 네트워크 호출, DB 접근, 메시지 처리에 적합
- 블로킹 스레드를 과도하게 늘리지 않으면서 많은 요청 처리 가능
- 동기 코드처럼 읽히는 비동기 로직 작성 가능

즉, 코루틴은 단순히 멀티스레딩을 정리하는 것이 아니라, 비동기 프로그램을 더 안전하고 이해하기 쉽게 만드는 개념입니다.

---

## 실행 방법

이 프로젝트는 Gradle + Kotlin JVM 기반입니다. IDE에서 각 파일의 `main` 함수를 실행하거나, 필요한 경우 Gradle 빌드를 통해 컴파일할 수 있습니다.

```bash
cd /coroutines/coroutines-2
./gradlew test
```

또는 IntelliJ IDEA / Android Studio에서 원하는 예제 파일의 `main` 함수를 Run으로 실행하면 됩니다.

예를 들어 아래와 같은 파일을 실행할 수 있습니다.

- `Main.kt`
- `Case03.kt`
- `Case04.kt`
- `Case05.kt`
- `Case06.kt`
- `Case07.kt`
- `Case08.kt`
- `Case09.kt`

---

## 마무리

이 프로젝트는 코루틴을 "이론 중심으로 배우는 것"에서 한 단계 더 나아가, 실제 코드에서 어떤 형태로 동작하는지 직접 눈으로 확인하는 데 초점을 둡니다.

핵심은 단순히 `delay()`를 쓰는 것이 아니라,

- 코루틴은 왜 필요한지
- 작업 흐름을 어떻게 중단하고 이어갈지
- 어떤 순간에 취소와 예외를 다뤄야 하는지
- 결과를 어떻게 조합해야 하는지

를 이해하는 것입니다.

코루틴은 한 번 익히면 네트워크, 데이터베이스, 서버, 클라이언트 개발 전반에서 매우 강력한 도구가 됩니다. 이 프로젝트는 그 첫걸음을 가장 직관적으로 설명하는 실습 자료로 활용할 수 있습니다.

---

## 참고

- Kotlin Coroutines 공식 문서
- `kotlinx.coroutines` 라이브러리
- 실무에서 자주 쓰는 패턴: `async/await`, `withContext`, `supervisorScope`, `runBlocking`, `flow`

다음 단계로는 `Flow`를 학습하면서, 비동기 데이터 스트림을 코루틴 기반으로 다루는 확장 개념까지 이어가면 좋습니다.
