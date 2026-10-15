package com.example.dbsdeposit

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
class DepositApiIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @Test
    fun `sync is idempotent and populates query cache`() {
        val partyId = "party-1001"
        val key = "idem-1001"

        mockMvc.post("/api/v1/parties/{partyId}/deposits/sync", partyId) {
            header("Idempotency-Key", key)
        }
            .andExpect {
                status { isOk() }
                header { string("Idempotent-Replayed", "false") }
                jsonPath("$.receivedCount") { value(2) }
                jsonPath("$.insertedCount") { value(2) }
            }

        mockMvc.post("/api/v1/parties/{partyId}/deposits/sync", partyId) {
            header("Idempotency-Key", key)
        }
            .andExpect {
                status { isOk() }
                header { string("Idempotent-Replayed", "true") }
                jsonPath("$.receivedCount") { value(2) }
                jsonPath("$.insertedCount") { value(2) }
            }

        mockMvc.get("/api/v1/parties/{partyId}/deposits", partyId)
            .andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(2) }
            }
    }

    @Test
    fun `same idempotency key for different party is rejected`() {
        val key = "shared-key"
        mockMvc.post("/api/v1/parties/p1/deposits/sync") {
            header("Idempotency-Key", key)
        }.andExpect { status { isOk() } }

        mockMvc.post("/api/v1/parties/p2/deposits/sync") {
            header("Idempotency-Key", key)
        }
            .andExpect {
                status { isUnprocessableEntity() }
                jsonPath("$.title") { value("IDEMPOTENCY_KEY_REUSED") }
            }
    }
}
