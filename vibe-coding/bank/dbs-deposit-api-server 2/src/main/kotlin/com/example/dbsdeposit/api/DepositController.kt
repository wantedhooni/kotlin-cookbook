package com.example.dbsdeposit.api

import com.example.dbsdeposit.api.model.DepositAccountResponse
import com.example.dbsdeposit.api.model.SyncDepositsResponse
import com.example.dbsdeposit.service.DepositQueryService
import com.example.dbsdeposit.service.DepositSyncService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/parties/{partyId}/deposits")
class DepositController(
    private val queryService: DepositQueryService,
    private val syncService: DepositSyncService,
) {
    @GetMapping
    fun list(@PathVariable partyId: String): List<DepositAccountResponse> = queryService.list(partyId)

    @GetMapping("/{accountId}")
    fun get(
        @PathVariable partyId: String,
        @PathVariable accountId: String,
    ): DepositAccountResponse = queryService.get(partyId, accountId)

    @PostMapping("/sync")
    fun sync(
        @PathVariable partyId: String,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
    ): ResponseEntity<SyncDepositsResponse> {
        val result = syncService.sync(partyId, idempotencyKey)
        return ResponseEntity.ok()
            .header("Idempotent-Replayed", result.replayed.toString())
            .body(result.body)
    }
}
