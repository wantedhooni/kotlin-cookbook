package com.example.bank.infrastructure

import com.example.bank.application.AuditPort
import com.example.bank.application.TransferPolicyPort
import com.example.bank.domain.BankException
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate

/** INSERT/UPSERT acquires a row lock on (account_id, day) to serialize daily limit checks. */
@Repository
class JdbcTransferPolicy(private val jdbc: JdbcTemplate,
    @Value("\${app.transfer.daily-limit:10000000.00}") private val dailyLimit: BigDecimal): TransferPolicyPort {
    override fun checkAndReserve(fromAccountId: Long, amount: BigDecimal) {
        val rows = jdbc.update("""INSERT INTO daily_transfer_usage(account_id, business_date, used_amount)
            VALUES (?, CURRENT_DATE, ?) ON CONFLICT(account_id,business_date) DO UPDATE
            SET used_amount=daily_transfer_usage.used_amount+EXCLUDED.used_amount
            WHERE daily_transfer_usage.used_amount+EXCLUDED.used_amount <= ?""",fromAccountId,amount,dailyLimit)
        if (rows != 1) throw BankException("DAILY_LIMIT_EXCEEDED", "Daily transfer limit exceeded")
    }
}
@Repository
class JdbcAuditAdapter(private val jdbc:JdbcTemplate): AuditPort {
    override fun record(subject:String,action:String,resource:String,detail:String) {
        jdbc.update("INSERT INTO audit_logs(subject,action,resource,detail) VALUES (?,?,?,?)", subject,action,resource,detail.take(1000))
    }
}
