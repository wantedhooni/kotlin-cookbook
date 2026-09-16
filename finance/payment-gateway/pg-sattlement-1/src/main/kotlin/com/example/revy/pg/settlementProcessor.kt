package com.example.revy.pg

@Bean
@StepScope
fun settlementProcessor(
    @Value("#{jobParameters['settlementDate']}")
    settlementDate: String,
): ItemProcessor<PgPaymentEntity, PgSettlementEntity> {
    val date = LocalDate.parse(settlementDate)

    return ItemProcessor { payment ->

        val order = payment.order

        val status =
            when {
                order == null -> {
                    SettlementStatus.ORDER_NOT_FOUND
                }

                order.paymentAmount.compareTo(payment.paymentAmount) != 0 -> {
                    SettlementStatus.AMOUNT_MISMATCH
                }

                else -> {
                    SettlementStatus.MATCHED
                }
            }

        PgSettlementEntity(
            pgTid = payment.pgTid,
            settlementDate = date,
            orderNo = payment.orderNo,
            paymentAmount = payment.paymentAmount,
            pgFee = payment.pgFee,
            settlementAmount =
                payment.paymentAmount - payment.pgFee,
            status = status,
        )
    }
}
