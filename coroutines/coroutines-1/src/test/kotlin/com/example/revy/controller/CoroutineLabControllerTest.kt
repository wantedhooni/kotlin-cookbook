package com.example.revy.controller

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.client.RestTestClient

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
@AutoConfigureRestTestClient
class CoroutineLabControllerTest {
    @Autowired
    private lateinit var restTestClient: RestTestClient

    @Test
    fun `blocking API 호출`() {
        restTestClient
            .get()
            .uri("/labs/coroutines/blocking")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(String::class.java)
            .isEqualTo("blocking")
    }
}
