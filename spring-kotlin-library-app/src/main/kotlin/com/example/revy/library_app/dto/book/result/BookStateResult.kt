package com.example.revy.library_app.dto.book.result

import com.example.revy.library_app.common.enums.BookType


data class BookStateResult(
    val type: BookType,
    val count: Long,
)
