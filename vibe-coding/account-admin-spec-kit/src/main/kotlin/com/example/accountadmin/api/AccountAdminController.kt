package com.example.accountadmin.api

import com.example.accountadmin.domain.AccountRole
import com.example.accountadmin.domain.AccountStatus
import com.example.accountadmin.service.AccountService
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.util.UUID

@Validated
@RestController
@RequestMapping("/api/admin/accounts")
class AccountAdminController(private val service: AccountService) {
    @GetMapping
    fun list(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) role: AccountRole?,
        @RequestParam(required = false) status: AccountStatus?,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int
    ) = service.list(keyword, role, status, page, size)

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID) = service.get(id)

    @PostMapping
    fun create(@Valid @RequestBody body: CreateAccountRequest): ResponseEntity<AccountResponse> {
        val created = service.create(body)
        return ResponseEntity.created(URI.create("/api/admin/accounts/${created.id}")).body(created)
    }

    @PutMapping("/{id}")
    fun update(@PathVariable id: UUID, @Valid @RequestBody body: UpdateAccountRequest) = service.update(id, body)

    @PatchMapping("/{id}/role")
    fun changeRole(@PathVariable id: UUID, @RequestBody body: ChangeRoleRequest) = service.changeRole(id, body)

    @PatchMapping("/{id}/status")
    fun changeStatus(@PathVariable id: UUID, @RequestBody body: ChangeStatusRequest) = service.changeStatus(id, body)

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: UUID): ResponseEntity<Void> {
        service.delete(id)
        return ResponseEntity.noContent().build()
    }
}
