package io.pleo.antaeus.core.services

import io.pleo.antaeus.data.PaymentDal
import java.math.BigDecimal

class PaymentService(
    private val dal: PaymentDal
) {
    suspend fun makePayment(invoiceId: Int, customerId: Int, subtractedBalanceValue: BigDecimal) {
        dal.makePayment(invoiceId, customerId, subtractedBalanceValue)
    }
}
