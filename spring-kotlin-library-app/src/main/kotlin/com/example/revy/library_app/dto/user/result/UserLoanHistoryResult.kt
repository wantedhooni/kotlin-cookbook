package com.example.revy.library_app.dto.user.result

import com.example.revy.library_app.domain.user.User
import com.example.revy.library_app.domain.user.UserLoanHistory

data class UserLoanHistoryResult(
    val name: String,  // 유저 이름
    val books: List<BookHistoryResponse>,
) {
    companion object {
        fun of(user: User): UserLoanHistoryResult {
            return UserLoanHistoryResult(
                name = user.name,
                books = user.userLoanHistories.map(BookHistoryResponse::of)
            )
        }
    }
}

data class BookHistoryResponse(
    val name: String,  // 책의 이름
    val isReturn: Boolean,
) {
    companion object {
        fun of(history: UserLoanHistory): BookHistoryResponse {
            return BookHistoryResponse(
                name = history.bookName,
                isReturn = history.isReturn
            )
        }
    }
}
