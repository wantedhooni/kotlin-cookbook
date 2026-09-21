# Kotlin 코루틴, 왜 써야 할까? 실습으로 이해하는 비동기 프로그래밍

최근 백엔드 개발을 하다 보면 한 번쯤 이런 고민을 해본 적이 있을 겁니다.

- 네트워크 호출이 많아서 응답 시간이 느려지는 건 아닌지
- DB 조회나 외부 API 호출을 병렬로 처리하고 싶은데 스레드 관리가 복잡하지는 않은지
- 비동기 코드를 동기 코드처럼 읽기 쉽게 작성할 수는 없는지

Kotlin 코루틴은 이런 고민을 해결해주는 강력한 도구입니다. 특히 `suspend`, `launch`, `async`, `withContext` 같은 개념을 알면 비동기 코드가 훨씬 자연스럽고 직관적으로 보이기 시작합니다.

이 글에서는 `coroutines-2` 프로젝트를 기준으로 코루틴의 핵심 개념을 실습 중심으로 정리해보겠습니다.

---

## 1. 코루틴이 뭔가요?

코루틴은 비동기 작업을 수행하는 경량 실행 단위입니다. 기존 스레드와 비교했을 때 핵심 차이는 다음과 같습니다.

- 스레드보다 훨씬 가볍게 생성되고 관리됨
- 작업을 일시 중단하고 다시 이어갈 수 있음
- `suspend` 함수로 비동기 로직을 동기 코드처럼 표현 가능
- 여러 작업을 동시에 실행하면서도 결과를 조합하기 편함

예를 들어, 사용자가 서버에 요청을 보낼 때 DB 조회와 이미지 조회를 각각 다른 서비스에서 가져와야 한다면, 코루틴을 사용하면 다음처럼 직관적으로 표현할 수 있습니다.

```kotlin
suspend fun findUser(userId: Long): UserDto {
    val profile = userProfileRepository.findProfile(userId)
    val image = userImageRepository.findImage(profile)
    return UserDto(profile, image)
}
```

이 코드는 실제로 각각의 호출이 비동기적으로 동작하더라도, 코드 흐름은 한 번에 읽히기 좋습니다.

---

## 2. 왜 스레드 대신 코루틴인가?

기존 자바/코틀린의 스레드 모델은 아주 강력하지만, 단점도 분명합니다.

- 스레드 수가 많아지면 메모리와 컨텍스트 스위칭 비용 증가
- 여러 작업을 동시에 처리할 때 복잡한 동기화 로직이 필요함
- 블로킹 I/O가 많은 프로그램에서 스레드가 쉽게 병목이 됨

반면 코루틴은 스레드를 많이 생성하지 않고도 많은 작업을 처리할 수 있습니다. 핵심은 코루틴이 "스레드가 아닌 작업 단위"로 실행되는 방식이기 때문입니다.

실제 코틀린은 스레드 풀 위에서 실행되지만, 코드를 작성할 때는 스레드를 직접 관리하는 대신 작업 흐름에 집중할 수 있습니다.

---

## 3. suspend 함수: 코루틴의 가장 기본 단위

코루틴의 핵심은 `suspend` 함수입니다.

```kotlin
suspend fun fetchUser(): User {
    delay(1000L)
    return User(1L, "revy")
}
```

`delay()`는 비동기적으로 잠시 멈추는 함수이지만, `suspend` 함수 안에서는 일반적인 코드처럼 읽히는 장점이 있습니다.

즉, 코드는 다음처럼 보이지만 실제로는 중단 가능하도록 설계된 것입니다.

- 호출 함수가 실행 중이다가 특정 지점에서 잠시 멈춤
- 이런 중단 상태를 JVM/런타임이 잘 관리함
- 이후 다시 이어서 실행

`delay`처럼 시간을 기다리는 작업은 스레드를 점유하고 멈추는 방식이 아니라, 흐름만 잠시 멈춘 뒤 이어가기 때문에 훨씬 효율적입니다.

---

## 4. `launch`와 `async` 차이

코루틴을 배울 때 가장 먼저 접하는 함수가 `launch`와 `async`입니다.

### `launch`

`launch`는 작업을 시작하고 결과를 바로 돌려주지 않습니다. 보통 "실행만 시키고 끝내는 작업"에 사용합니다.

```kotlin
launch {
    delay(1000L)
    println("작업 완료")
}
```

이 경우 결과값이 필요 없고, 그냥 백그라운드에서 수행하면 됩니다.

### `async`

`async`는 결과를 `Deferred`로 반환합니다. 이후 `await()`로 값을 받습니다.

```kotlin
val total = async {
    val a = apiCall1()
    val b = apiCall2()
    a + b
}

println(total.await())
```

이 패턴은 여러 작업을 병렬로 실행하고 최종 결과를 조합할 때 매우 유용합니다. `coroutines-2` 프로젝트의 `Case08.kt`에서도 `async`와 `await`를 활용한 예를 볼 수 있습니다.

```kotlin
fun lec08Example4(): Unit = runBlocking {
    printWithThread("START")
    printWithThread(calculateResult())
    printWithThread("END")
}

suspend fun calculateResult(): Int = withContext(Dispatchers.Default) {
    val num1 = async {
        delay(1_000L)
        10
    }

    val num2 = async {
        delay(1_000L)
        20
    }

    num1.await() + num2.await()
}
```

여기서 핵심은 두 비동기 계산을 동시에 시작하고, 마지막에 합쳐서 결과를 받아온다는 점입니다. 코드처럼 흐름이 보이기 때문에 이해가 쉽습니다.

---

## 5. `delay`와 `yield`: 코루틴은 협업적으로 움직인다

코루틴은 단순히 "잠깐 멈추고 다시 실행되기"만 하는 것이 아닙니다. 실행 흐름을 적극적으로 양보하고, 다른 코루틴이 작업을 이어받을 수 있게 합니다.

### `delay`

```kotlin
launch {
    println("START")
    delay(500L)
    println("END")
}
```

`delay`는 특정 시간 동안 코루틴을 일시 중단합니다. 이때 스레드를 막는 것이 아니라 다른 작업이 실행될 수 있는 여지를 제공합니다.

### `yield`

`yield`는 현재 코루틴이 다른 코루틴에게 실행 기회를 넘기고 싶을 때 사용합니다.

```kotlin
suspend fun newRoutine() {
    val num1 = 1
    val num2 = 2
    yield()
    printWithThread("${num1 + num2}")
}
```

`yield`는 특히 협업적 스케줄링을 구현할 때 유용합니다. 어떤 코루틴이 너무 오래 CPU를 차지하지 않도록 중간에 양보해주는 구조를 만들 수 있습니다.

`Case09.kt`는 이런 흐름을 가장 단순하고 명확하게 보여줍니다. `runBlocking` 안에서 `launch`로 새로운 코루틴을 열고, `yield`로 실행 흐름을 제어합니다.

---

## 6. `runBlocking`은 무엇을 위한가?

코루틴을 공부할 때 가장 많이 보는 함수 중 하나가 바로 `runBlocking`입니다.

```kotlin
fun main() = runBlocking {
    println("START")
    launch {
        delay(1000L)
        println("코루틴 실행")
    }
    println("END")
}
```

`runBlocking`은 코루틴을 블로킹하는 구조를 만들고, 메인 스레드에서 코루틴을 실행시킬 때 사용합니다. 보통 메인 함수나 테스트 환경에서 가장 쉽게 코루틴을 시작하는 방식으로 많이 쓰입니다.

즉, `runBlocking`은 "여기까지는 메인 스레드가 블로킹된다"는 뜻을 가진 함수이지만, 코루틴 자체를 시작하고 관리하는 용도로 자주 사용됩니다.

이 프로젝트에서도 `Case03.kt`, `Case04.kt`, `Case05.kt` 등 거의 모든 예제가 `runBlocking` 내부에서 시작되는 형태를 취하고 있습니다.

---

## 7. 취소(Cancel)와 예외 처리

코루틴은 단순히 작업이 끝나면 종료되는 것이 아니라, 외부에서 취소될 수 있습니다.

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

이 코드에서 중요한 점은 취소가 곧바로 쓰레드를 종료하는 것이 아니라, 예외로 처리될 수 있다는 점입니다.

### `CancellationException`

`job.cancel()`을 호출하면 코루틴은 `CancellationException`을 받게 되고, 필요하다면 `finally` 블록에서 정리 작업을 수행할 수 있습니다.

이는 스레드를 강제 종료하는 것보다 훨씬 안전합니다. 특히 파일, DB 연결, 네트워크 세션 같은 자원을 정리할 때 중요합니다.

### `CoroutineExceptionHandler`

예외를 전역적으로 처리할 때 `CoroutineExceptionHandler`를 사용할 수 있습니다.

```kotlin
val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
    println("예외 발생: $throwable")
}

launch(exceptionHandler) {
    throw IllegalArgumentException()
}
```

`Case05.kt`에는 `CoroutineExceptionHandler`, `SupervisorJob`, `try-catch` 조합 예제가 포함되어 있어서, 예외가 자식 코루틴 전체로 전파되는 구조를 이해하는 데 매우 좋습니다.

---

## 8. `SupervisorJob`는 왜 필요할까?

기본적으로 코루틴 부모-자식 관계에서는 자식 하나가 실패하면 부모까지 예외가 전파되고, 전체가 취소될 수 있습니다.

```kotlin
val job = async(SupervisorJob()) {
    throw IllegalArgumentException()
}
```

`SupervisorJob`는 자식 코루틴의 실패가 다른 자식들에 영향을 주지 않도록 도와줍니다.

예를 들어 여러 API를 병렬로 호출하는 상황에서 하나만 실패해도 전체 작업이 멈추면 안 되는 경우가 많습니다. 이때 `SupervisorJob`는 매우 유용합니다.

실무에서는 다음 같은 흐름이 자주 보입니다.

- 사용자 정보 조회 중 일부는 실패해도 다른 데이터는 계속 받아오기
- 로깅 작업은 독립적으로 실패해도 전체 트랜잭션 중단 방지
- 메일 전송, 알림 전송 등 독립적인 작업을 분리하여 처리

즉, 코루틴 예외 구조를 이해하는 것은 복잡한 비동기 시스템을 안정적으로 운영하는 데 큰 도움이 됩니다.

---

## 9. `CoroutineScope`와 `Dispatchers`

코루틴은 언제, 어디서 실행될지를 결정할 수 있어야 합니다. 이를 위해 `CoroutineScope`와 `Dispatchers`가 사용됩니다.

### `CoroutineScope`

코루틴의 생명주기를 관리하는 범위입니다.

```kotlin
private val scope = CoroutineScope(Dispatchers.Default)
```

이 스코프를 취소하면 그 안의 코루틴들이 함께 종료됩니다.

```kotlin
fun destroy() {
    scope.cancel()
}
```

이 패턴은 Android ViewModel, Repository, Service 같은 객체에서 매우 자주 사용됩니다.

### `Dispatchers`

`Dispatchers`는 코루틴이 어떤 스레드에서 실행될지를 정하는 요소입니다.

- `Dispatchers.Main`: UI 스레드
- `Dispatchers.IO`: I/O 집약 작업
- `Dispatchers.Default`: CPU 집약 작업

```kotlin
CoroutineScope(Dispatchers.Default).launch {
    // 백그라운드 스레드에서 실행
}
```

이런 분리를 통해 UI는 UI 스레드에서, 무거운 작업은 백그라운드에서 처리하는 방식으로 프로그램을 설계할 수 있습니다.

---

## 10. `withContext`와 비동기 결과 조합

코루틴의 가장 강력한 장점 중 하나는 여러 비동기 작업을 하나의 흐름으로 조합할 수 있다는 것입니다.

```kotlin
suspend fun calculateResult(): Int = withContext(Dispatchers.Default) {
    val num1 = async { fetchA() }
    val num2 = async { fetchB() }
    num1.await() + num2.await()
}
```

이 코드는 다음을 나타냅니다.

- CPU 집약 작업이나 I/O 작업을 다른 디스패처에서 처리
- 다음으로 두 비동기 작업을 동시에 시작
- 마지막에 둘의 결과를 합산

실제로는 다수의 외부 API/DB 호출을 동시에 시작하고, 결과를 합치거나 정리하는 데 많이 사용됩니다.

`Case08.kt`에는 이런 패턴이 잘 담겨 있으며, 실제로 매우 실용적인 예제입니다.

---

## 11. `UserServiceV2`는 코루틴의 실전 감각을 보여준다

`coroutines-2` 프로젝트에는 `UserServiceV2.kt`가 있습니다. 이 파일은 사용자 정보를 가져오는 로직을 보여주는데, 핵심은 다음과 같습니다.

```kotlin
suspend fun findUser(userId: Long): UserDtoV2 {
    println("유저를 가져오겠습니다")
    val profile = userProfileRepository.findProfile(userId)
    println("이미지를 가져오겠습니다")
    val image = userImageRepository.findImage(profile)
    return UserDtoV2(profile, image)
}
```

이 코드는 실제 서버에서 자주 볼 수 있는 구조입니다.

- 사용자의 기본 정보 조회
- 추가 정보 조회
- 최종적으로 하나의 DTO 만들기

여기서 가장 중요한 점은 비동기 호출이 많아도 코드를 순차적으로 읽을 수 있다는 것입니다. `suspend` 함수를 활용하면 I/O가 많아도 로직의 흐름이 깔끔하게 유지됩니다.

즉, 동기 코드처럼 읽히지만 실제로는 중단 가능하고 비동기적으로 동작하는 구조를 만드는 것이 코루틴의 가장 큰 장점입니다.

---

## 12. 코루틴을 배우는 이유: 코드가 단순해지고, 흐름이 보인다

처음 코루틴을 접하면 `launch`, `async`, `withContext`, `cancel`, `SupervisorJob` 같은 이름이 많아 복잡하게 느껴집니다.

하지만 실무에서 가장 큰 변화는 코드의 가독성입니다.

- 비동기 로직이 동기 코드처럼 읽힘
- 스레드 관리에 신경 쓰지 않아도 됨
- 여러 네트워크/DB 호출을 자연스럽게 조합 가능
- 취소와 예외 흐름을 구조적으로 다룰 수 있음

코루틴은 단지 스레드 대체 기술이 아니라, 비동기 프로그래밍을 더 안전하고 명확하게 만들어주는 프로그래밍 패턴입니다.

---

## 13. 이런 상황에서 코루틴이 특히 유용하다

코루틴은 다음과 같은 상황에서 특히 강력합니다.

- 외부 API를 여러 개 동시에 호출해야 할 때
- DB 조회와 캐시 조회를 병렬로 처리할 때
- UI에서 네트워크 응답을 기다리면서 사용자 경험을 유지해야 할 때
- 백엔드 서버에서 많은 요청을 효율적으로 처리해야 할 때
- 취소와 시간 초과를 정교하게 제어해야 할 때

요약하면, 비동기 처리가 많은 프로그램이라면 코루틴은 거의 필수적인 도구에 가깝습니다.

---

## 14. 마무리

`coroutines-2` 프로젝트는 코루틴을 이해하는 데 아주 좋은 실습 자료입니다.

- `launch`를 통해 백그라운드 작업 실행을 이해하고
- `async`로 결과를 조합하고
- `delay`와 `yield`로 실행 흐름을 제어하고
- `cancel`과 `SupervisorJob`으로 실패와 취소를 다루고
- `withContext`로 작업을 적절한 컨텍스트로 분리하는

이 과정을 코드로 직접 확인할 수 있습니다.

코루틴은 처음에는 약간 추상적으로 느껴질 수 있지만, 한 번 익숙해지면 비동기 프로그래밍을 훨씬 명확하게 표현할 수 있습니다.

특히 Kotlin에서는 코루틴이 단순한 라이브러리가 아니라, 언어와 가장 자연스럽게 조화되는 비동기 설계 패턴으로 자리잡고 있습니다.

다음 단계로는 이 프로젝트에서 다룬 내용을 바탕으로 `Flow`까지 확장해보면, 비동기 데이터 스트림을 더 효과적으로 처리하는 방법도 체득할 수 있습니다.

코루틴은 단지 “비동기 코드”가 아니라, 코드가 어떻게 흐르는지를 더 읽기 쉽게 만드는 도구입니다. 그 핵심을 이번 프로젝트가 잘 보여줍니다.

---

## 참고 코드

- `Case03.kt` : `launch`로 동시 실행 이해
- `Case04.kt` : 취소와 정리
- `Case05.kt` : 예외 처리와 `SupervisorJob`
- `Case08.kt` : `async`와 `withContext`
- `Case09.kt` : `yield` 기반 흐름 제어
- `UserServiceV2.kt` : 실제 비동기 사용자 조회 흐름

이 시리즈를 이어서 보면, Kotlin 코루틴의 기본기를 탄탄하게 쌓을 수 있습니다.
