package com.example.accountadmin.service

import com.example.accountadmin.api.*
import com.example.accountadmin.domain.Account
import com.example.accountadmin.domain.AccountRole
import com.example.accountadmin.domain.AccountStatus
import com.example.accountadmin.repository.AccountRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.springframework.context.ApplicationEventPublisher
import java.util.Optional
import java.util.UUID

class AccountServiceTest : BehaviorSpec({
    val repository = mockk<AccountRepository>()
    val publisher = mockk<ApplicationEventPublisher>(relaxed = true)
    val service = AccountService(repository, publisher)

    Given("a new account request") {
        val request = CreateAccountRequest(" Tester ", " TEST@Example.com ", "Test User")
        When("the login id and email are available") {
            clearMocks(repository, publisher)
            every { repository.existsByLoginId("tester") } returns false
            every { repository.existsByEmail("test@example.com") } returns false
            every { repository.save(any()) } answers { firstArg() }
            val created = service.create(request)
            Then("it normalizes identifiers and emits an audit event") {
                created.loginId shouldBe "tester"
                created.email shouldBe "test@example.com"
                created.status shouldBe AccountStatus.ACTIVE
                verify(exactly = 1) { publisher.publishEvent(match<AccountAuditEvent> { it.action == "ACCOUNT_CREATED" }) }
            }
        }
        When("the login id already exists") {
            clearMocks(repository, publisher)
            every { repository.existsByLoginId("tester") } returns true
            Then("it rejects the duplicate") {
                shouldThrow<DuplicateAccountException> { service.create(request) }
                verify(exactly = 0) { repository.save(any()) }
            }
        }
    }

    Given("an existing account") {
        val id = UUID.randomUUID()
        val account = Account(id = id, loginId = "member", email = "user@example.com", displayName = "Member")
        When("it is suspended") {
            clearMocks(repository, publisher)
            account.status = AccountStatus.ACTIVE
            every { repository.findById(id) } returns Optional.of(account)
            val result = service.changeStatus(id, ChangeStatusRequest(AccountStatus.SUSPENDED))
            Then("it changes the status and emits an event") {
                result.status shouldBe AccountStatus.SUSPENDED
                verify { publisher.publishEvent(match<AccountAuditEvent> { it.action == "STATUS_CHANGED" }) }
            }
        }
        When("it is deleted") {
            clearMocks(repository, publisher)
            account.status = AccountStatus.ACTIVE
            every { repository.findById(id) } returns Optional.of(account)
            service.delete(id)
            Then("it is soft-deleted and no longer readable") {
                account.status shouldBe AccountStatus.DELETED
                shouldThrow<AccountNotFoundException> { service.get(id) }
                verify { publisher.publishEvent(match<AccountAuditEvent> { it.action == "ACCOUNT_DELETED" }) }
            }
        }
        When("a deleted state is requested through the status API") {
            clearMocks(repository, publisher)
            Then("it refuses that transition") {
                shouldThrow<InvalidAccountStateException> {
                    service.changeStatus(id, ChangeStatusRequest(AccountStatus.DELETED))
                }
            }
        }
    }

    Given("an unknown account") {
        val id = UUID.randomUUID()
        every { repository.findById(id) } returns Optional.empty()
        When("it is read") {
            Then("it returns a not-found error") {
                shouldThrow<AccountNotFoundException> { service.get(id) }
            }
        }
    }
})
