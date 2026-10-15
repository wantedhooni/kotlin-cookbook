package com.example.trs.repository

import com.example.trs.domain.QRefund.refund
import com.example.trs.domain.Refund
import com.example.trs.domain.RefundStatus
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

@Repository
class RefundQueryRepositoryImpl(private val queryFactory: JPAQueryFactory) : RefundQueryRepository {
    override fun search(status: RefundStatus?, merchantId: Long?): List<Refund> =
        queryFactory.selectFrom(refund)
            .join(refund.document).fetchJoin()
            .join(refund.document.purchase).fetchJoin()
            .where(
                status?.let { refund.status.eq(it) },
                merchantId?.let { refund.document.purchase.merchant.id.eq(it) }
            )
            .orderBy(refund.id.desc())
            .fetch()
}
