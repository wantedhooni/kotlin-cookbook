package com.example.bank.infrastructure

import com.example.bank.domain.*
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity @Table(name="banks")
class BankEntity(@Column(nullable=false,unique=true) var code:String="", var name:String="",
                 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) var id:Long?=null)
@Entity @Table(name="accounts")
class AccountEntity(
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="bank_id") var bank:BankEntity=BankEntity(),
    @Column(name="account_number",unique=true) var accountNumber:String="",
    @Column(name="owner_name") var ownerName:String="",
    @Column(precision=19,scale=2) var balance:BigDecimal=BigDecimal.ZERO.setScale(2),
    @Enumerated(EnumType.STRING) var status:AccountStatus=AccountStatus.ACTIVE,
    @Version var version:Long=0,
    @Column(name="created_at") var createdAt:Instant=Instant.now(),
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) var id:Long?=null)
@Entity @Table(name="ledger_postings")
class PostingEntity(
    @Column(name="reference_id") var referenceId:UUID=UUID.randomUUID(),
    @Column(name="account_id") var accountId:Long?=null,
    @Enumerated(EnumType.STRING) var book:Book=Book.CUSTOMER,
    @Enumerated(EnumType.STRING) var side:EntrySide=EntrySide.CREDIT,
    @Column(precision=19,scale=2) var amount:BigDecimal=BigDecimal.ONE,
    @Column(name="created_at") var createdAt:Instant=Instant.now(),
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) var id:Long?=null)
@Entity @Table(name="outbox_events")
class OutboxEntity(@Id var id:UUID=UUID.randomUUID(), @Column(name="aggregate_id") var aggregateId:Long=0,
    @Column(name="event_type") var eventType:String="", @Column(columnDefinition="text") var payload:String="",
    @Column(name="created_at") var createdAt:Instant=Instant.now(), @Column(name="published_at") var publishedAt:Instant?=null)
fun AccountEntity.toDomain() = Account(requireNotNull(id),bank.code,accountNumber,ownerName,balance,status)
