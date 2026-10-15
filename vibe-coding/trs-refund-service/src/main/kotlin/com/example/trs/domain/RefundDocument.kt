package com.example.trs.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "refund_documents", uniqueConstraints = [UniqueConstraint(name = "uk_document_number", columnNames = ["document_number"])])
class RefundDocument(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_id", nullable = false)
    var purchase: Purchase,
    @Column(name = "document_number", nullable = false)
    var documentNumber: String,
    @Column(nullable = false)
    var issuedAt: LocalDateTime = LocalDateTime.now(),
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
)
