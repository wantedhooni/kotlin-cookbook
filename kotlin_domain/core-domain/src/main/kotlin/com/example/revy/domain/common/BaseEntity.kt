package com.example.revy.domain.common

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import org.hibernate.Hibernate
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.io.Serializable
import java.time.Instant
import java.util.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid


@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity : Serializable {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    open var id: Long? = null;

    @OptIn(ExperimentalUuidApi::class)
    @Column(name = "oid", columnDefinition = "uuid", nullable = false, unique = true)
    var oid: UUID? = Uuid.generateV7().toJavaUuid()
        protected set

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    var createdAt: Instant = Instant.now()

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP")
    var updatedAt: Instant = Instant.now()
        protected set

    val isNew: Boolean
        get() = this.id == null

    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        // 프록시 대응: 실제 클래스 비교
        val thisClass = Hibernate.getClass(this)
        val otherClass = Hibernate.getClass(other)
        if (thisClass != otherClass) return false
        other as BaseEntity
        return id != null && id == other.id
    }

    // ★ 상수 반환: 영속화 전(id=null) → 영속화 후(id 할당) 해시가 바뀌면
    //   HashSet/HashMap에서 엔티티를 잃어버림
    final override fun hashCode(): Int = Hibernate.getClass(this).hashCode()
    override fun toString(): String {
        return "BaseEntity(id=$id, oid=$oid, createdAt=$createdAt, updatedAt=$updatedAt, isNew=$isNew)"
    }

}