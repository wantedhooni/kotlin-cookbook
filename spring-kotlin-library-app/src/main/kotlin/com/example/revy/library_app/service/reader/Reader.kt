package com.example.revy.library_app.service.reader

import com.example.revy.library_app.dto.book.result.BookStateResult
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import com.example.revy.library_app.domain.book.QBook
import com.example.revy.library_app.common.enums.UserLoanStatus
import com.example.revy.library_app.domain.user.QUserLoanHistory
import com.example.revy.library_app.domain.user.UserLoanHistory

import org.springframework.stereotype.Component

@Component
class UserLoanHistoryReader(
    private val queryFactory: JPAQueryFactory,
) {
    private val userLoanHistory = QUserLoanHistory.userLoanHistory

    fun find(bookName: String, status: UserLoanStatus?): UserLoanHistory? {
        return queryFactory.select(userLoanHistory)
            .from(userLoanHistory)
            .where(
                userLoanHistory.bookName.eq(bookName),
                status?.let { userLoanHistory.status.eq(status) }
            )
            .limit(1)
            .fetchOne()
    }

    fun count(status: UserLoanStatus): Long {
        return queryFactory.select(userLoanHistory.count())
            .from(userLoanHistory)
            .where(
                userLoanHistory.status.eq(status)
            )
            .fetchOne() ?: 0L
    }

}

@Component
class BookReader(
    private val queryFactory: JPAQueryFactory,
) {
    private val book = QBook.book
    fun getStats(): List<BookStateResult> {
        return queryFactory
            .select(
                Projections.constructor(
                    BookStateResult::class.java,
                    book.type,
                    book.id.count()
                )
            )
            .from(book)
            .groupBy(book.type)
            .fetch()
    }

}