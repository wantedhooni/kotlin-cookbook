package com.example.trs.repository

import com.example.trs.domain.Refund
import com.example.trs.domain.RefundStatus

interface RefundQueryRepository {
    fun search(status: RefundStatus?, merchantId: Long?): List<Refund>
}
