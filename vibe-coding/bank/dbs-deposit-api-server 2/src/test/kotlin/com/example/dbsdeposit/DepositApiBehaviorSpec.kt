package com.example.dbsdeposit

import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
@ApplyExtension(SpringExtension::class)
class DepositApiBehaviorSpec : BehaviorSpec() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    init {
        Given("아직 동기화되지 않은 고객에 유효한 멱등성 키가 주어졌을 때") {
            When("동일한 동기화 요청을 같은 멱등성 키로 두 번 호출하면") {
                Then("첫 번째 요청만 실제 처리하고 두 번째 요청은 저장된 응답을 재사용한다") {
                    val partyId = "bdd-party-1001"
                    val key = "bdd-idem-1001"

                    mockMvc.post("/api/v1/parties/{partyId}/deposits/sync", partyId) {
                        header("Idempotency-Key", key)
                    }.andExpect {
                        status { isOk() }
                        header { string("Idempotent-Replayed", "false") }
                        jsonPath("$.partyId") { value(partyId) }
                        jsonPath("$.receivedCount") { value(2) }
                        jsonPath("$.insertedCount") { value(2) }
                    }

                    mockMvc.post("/api/v1/parties/{partyId}/deposits/sync", partyId) {
                        header("Idempotency-Key", key)
                    }.andExpect {
                        status { isOk() }
                        header { string("Idempotent-Replayed", "true") }
                        jsonPath("$.partyId") { value(partyId) }
                        jsonPath("$.receivedCount") { value(2) }
                        jsonPath("$.insertedCount") { value(2) }
                    }

                    mockMvc.get("/api/v1/parties/{partyId}/deposits", partyId)
                        .andExpect {
                            status { isOk() }
                            jsonPath("$.length()") { value(2) }
                        }
                }
            }
        }

        Given("이미 다른 요청에서 사용된 멱등성 키가 있을 때") {
            When("같은 멱등성 키를 다른 고객의 동기화 요청에 재사용하면") {
                Then("요청 해시가 일치하지 않아 422 IDEMPOTENCY_KEY_REUSED를 반환한다") {
                    val key = "bdd-shared-key"

                    mockMvc.post("/api/v1/parties/bdd-p1/deposits/sync") {
                        header("Idempotency-Key", key)
                    }.andExpect {
                        status { isOk() }
                    }

                    mockMvc.post("/api/v1/parties/bdd-p2/deposits/sync") {
                        header("Idempotency-Key", key)
                    }.andExpect {
                        status { isUnprocessableEntity() }
                        jsonPath("$.title") { value("IDEMPOTENCY_KEY_REUSED") }
                    }
                }
            }
        }

        Given("예금계좌 동기화 API를 호출할 때") {
            When("멱등성 키 헤더가 누락되면") {
                Then("400 BAD_REQUEST를 반환한다") {
                    mockMvc.post("/api/v1/parties/bdd-no-header/deposits/sync")
                        .andExpect {
                            status { isBadRequest() }
                            jsonPath("$.title") { value("BAD_REQUEST") }
                        }
                }
            }

            When("멱등성 키가 빈 문자열이면") {
                Then("400 BAD_REQUEST를 반환한다") {
                    mockMvc.post("/api/v1/parties/bdd-blank-key/deposits/sync") {
                        header("Idempotency-Key", "")
                    }.andExpect {
                        status { isBadRequest() }
                        jsonPath("$.title") { value("BAD_REQUEST") }
                    }
                }
            }
        }
    }
}
