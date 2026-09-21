package com.example.revy.library_app.domain.book

import com.example.revy.library_app.common.enums.BookType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
class Book(
    val name: String,

    @Enumerated(EnumType.STRING)
    val type: BookType,

    // 디폴트 파라미터는 관례상 가장 밑에 위치
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
) {

    init {
        if (name.isBlank()) {
            throw IllegalArgumentException("이름은 비어 있을 수 없습니다")
        }
    }

    // 도메인 레이어가 변경돼도 테스트 코드를 수정할 필요가 없음
    companion object {
        // 이런 테스트에 이용할 객체를 만드는 함수를 object mother라고 부른다
        // -> 생겨난 테스트용 객체는 text fixture이라고 한다.
        fun fixture(
            name: String = "책 이름",
            type: BookType = BookType.COMPUTER,
            id: Long? = null,
        ): Book {
            return Book(
                name = name,
                type = type,
                id = id
            )
        }
    }

}