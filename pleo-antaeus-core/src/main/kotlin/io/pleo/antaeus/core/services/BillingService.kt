package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.InvoiceStatus

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService,
    private val dal: AntaeusDal
) {
    fun chargeBills() {
        val pendingInvoices = invoiceService.fetchAllByStatus(InvoiceStatus.PENDING)
        pendingInvoices.forEach{
            if (paymentProvider.charge(it)) {
                dal.makePayment(it.id, it.customerId, it.amount.value)
            }
        }
    }
}
