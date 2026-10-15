package com.example.accountadmin.api

import com.example.accountadmin.domain.AccountRole
import com.example.accountadmin.domain.AccountStatus
import com.example.accountadmin.service.AccountService
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.http.HttpStatus

class AccountAdminControllerTest : BehaviorSpec({
    val service = mockk<AccountService>()
    val controller = AccountAdminController(service)

    Given("an account list endpoint") {
        every { service.list("alice", AccountRole.USER, AccountStatus.ACTIVE, 0, 20) } returns
            AccountPageResponse(emptyList(), 0, 20, 0, 0)
        When("a keyword, role and status are supplied") {
            val result = controller.list("alice", AccountRole.USER, AccountStatus.ACTIVE, 0, 20)
            Then("the filters are forwarded to the service") {
                result.totalElements shouldBe 0L
                verify(exactly = 1) { service.list("alice", AccountRole.USER, AccountStatus.ACTIVE, 0, 20) }
            }
        }
    }

    Given("a create request") {
        val request = CreateAccountRequest("alice", "alice@example.com", "Alice")
        val response = AccountResponse(java.util.UUID.randomUUID(), "alice", "alice@example.com", "Alice",
            AccountRole.USER, AccountStatus.ACTIVE, java.time.Instant.now(), java.time.Instant.now())
        every { service.create(request) } returns response
        When("creation succeeds") {
            val result = controller.create(request)
            Then("the endpoint returns 201 and a Location header") {
                result.statusCode shouldBe HttpStatus.CREATED
                result.headers.location?.path shouldBe "/api/admin/accounts/${response.id}"
            }
        }
    }
})
