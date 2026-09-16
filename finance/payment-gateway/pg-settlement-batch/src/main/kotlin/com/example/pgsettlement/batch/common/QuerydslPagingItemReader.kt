package com.example.pgsettlement.batch.common

import com.example.pgsettlement.common.logger
import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import org.slf4j.LoggerFactory
import org.springframework.batch.infrastructure.item.database.AbstractPagingItemReader
import org.springframework.stereotype.Component

/**
 * Spring Batch의 AbstractPagingItemReader를 QueryDSL JPA 쿼리로 연결한 단순 offset paging Reader.
 *
 * 주의:
 * - 정렬 기준은 반드시 deterministic 해야 한다.
 * - 배치 실행 중 원본 데이터가 끼어들 수 있는 대규모/고동시성 환경에서는 keyset(no-offset) 방식이 더 안전하다.
 */
open class QuerydslPagingItemReader<T : Any>(
    private val entityManagerFactory: EntityManagerFactory,
    name: String,
    pageSize: Int,
    private val queryProvider: (JPAQueryFactory) -> JPAQuery<T>,
) : AbstractPagingItemReader<T>() {
    private val log by logger()

    private var entityManager: EntityManager? = null

    init {
        setName(name)
        setPageSize(pageSize)
    }

    override fun doOpen() {
        super.doOpen()
        entityManager = entityManagerFactory.createEntityManager()
    }

    override fun doReadPage() {
        val em = requireNotNull(entityManager) { "EntityManager is not opened" }
        val queryFactory = JPAQueryFactory(em)

        results =
            queryProvider(queryFactory)
                .offset(page.toLong() * pageSize.toLong())
                .limit(pageSize.toLong())
                .fetch()

        // Projection 위주의 reader이지만 장시간 실행 시 영속성 컨텍스트 누적을 방지한다.
        em.clear()
    }

    override fun doClose() {
        try {
            entityManager?.close()
            entityManager = null
        } finally {
            super.doClose()
        }
    }
}
