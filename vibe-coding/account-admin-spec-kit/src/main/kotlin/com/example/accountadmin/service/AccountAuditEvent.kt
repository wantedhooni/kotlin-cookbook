package com.example.accountadmin.service

import java.time.Instant
import java.util.UUID

data class AccountAuditEvent(
    val accountId: UUID,
    val action: String,
    val performedBy: String,
    val occurredAt: Instant = Instant.now()
)
