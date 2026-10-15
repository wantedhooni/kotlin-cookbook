package com.example.bank.infrastructure

import com.example.bank.application.*
import com.example.bank.domain.*
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.util.UUID

interface BanksJpa:JpaRepository<BankEntity,Long> { fun findByCode(code:String):BankEntity? }
interface AccountsJpa:JpaRepository<AccountEntity,Long> {
    fun existsByAccountNumber(accountNumber:String):Boolean
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AccountEntity a join fetch a.bank where a.id = :id")
    fun lockOne(@Param("id") id:Long):AccountEntity?
}
interface PostingsJpa:JpaRepository<PostingEntity,Long> {
    fun findByAccountIdOrderByIdDesc(accountId:Long):List<PostingEntity>
}
interface OutboxJpa:JpaRepository<OutboxEntity,UUID> {
    fun findTop100ByPublishedAtIsNullOrderByCreatedAtAsc():List<OutboxEntity>
}
@Repository
class JpaAccountAdapter(private val banks:BanksJpa,private val accounts:AccountsJpa,private val em:EntityManager):AccountPort {
    override fun open(bankCode:String,number:String,owner:String):Account {
        val bank = banks.findByCode(bankCode) ?: throw BankException("BANK_NOT_FOUND", "Bank not found")
        if (number.isBlank() || owner.isBlank()) throw BankException("INVALID_REQUEST", "Number and owner required")
        if (accounts.existsByAccountNumber(number)) throw BankException("DUPLICATE_ACCOUNT", "Account already exists")
        return accounts.saveAndFlush(AccountEntity(bank=bank,accountNumber=number,ownerName=owner)).toDomain()
    }
    override fun find(id:Long):Account? = accounts.findById(id).orElse(null)?.toDomain()
    override fun lock(id:Long):Account? = accounts.lockOne(id)?.toDomain()
    override fun save(account:Account):Account {
        val entity=em.find(AccountEntity::class.java,account.id) ?: throw BankException("ACCOUNT_NOT_FOUND", "Account not found")
        entity.balance=account.balance
        return entity.toDomain()
    }
    override fun search(bankCode:String?,owner:String?,status:AccountStatus?):List<Account> {
        val a=QAccountEntity.accountEntity
        val b=QBankEntity.bankEntity
        return JPAQueryFactory(em).selectFrom(a).join(a.bank,b).fetchJoin()
            .where(bankCode?.takeIf{it.isNotBlank()}?.let{b.code.eq(it)},
                owner?.takeIf{it.isNotBlank()}?.let{a.ownerName.containsIgnoreCase(it)},
                status?.let{a.status.eq(it)})
            .orderBy(a.id.desc()).limit(100).fetch().map{it.toDomain()}
    }
}
@Repository
class JdbcTransferAdapter(private val jdbc:JdbcTemplate):TransferPort {
    override fun claim(command:TransferCommand):Boolean = jdbc.update(
        """INSERT INTO transfers(request_id, from_account_id, to_account_id, amount)
           VALUES (?, ?, ?, ?) ON CONFLICT (request_id) DO NOTHING""".trimIndent(),
        command.requestId, command.fromId, command.toId, command.amount) == 1
    override fun find(requestId:UUID):StoredTransfer? = jdbc.query(
        "SELECT from_account_id, to_account_id, amount, reference_id FROM transfers WHERE request_id = ?",
        { rs:ResultSet,_:Int -> StoredTransfer(rs.getLong(1),rs.getLong(2),rs.getBigDecimal(3),rs.getObject(4,UUID::class.java)) },requestId
    ).firstOrNull()
    override fun complete(requestId:UUID,referenceId:UUID) {
        check(jdbc.update("UPDATE transfers SET reference_id = ? WHERE request_id = ?",referenceId,requestId)==1)
    }
}
@Repository
class JpaLedgerAdapter(private val postings:PostingsJpa):LedgerPort {
    override fun append(journal:Journal) {
        postings.saveAll(journal.postings.map{PostingEntity(referenceId=journal.referenceId,
            accountId=it.accountId,book=it.book,side=it.side,amount=it.amount)})
    }
    override fun history(accountId:Long):List<TransactionRecord> = postings.findByAccountIdOrderByIdDesc(accountId)
        .map{TransactionRecord(requireNotNull(it.id),accountId,it.referenceId,it.side,it.amount)}
}
