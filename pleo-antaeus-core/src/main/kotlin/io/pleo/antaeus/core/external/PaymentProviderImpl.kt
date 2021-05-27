package io.pleo.antaeus.core.external

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.SideUpdateException
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class PaymentProviderImpl(
    private val customerService: CustomerService
) : PaymentProvider {

    @Throws(CustomerNotFoundException::class)
    override suspend fun charge(invoice: Invoice): Boolean {
        if (InvoiceStatus.PAID == invoice.status) {
            throw SideUpdateException(invoice.id)
        }

        val customer = customerService.fetch(invoice.customerId)

        if (invoice.amount.currency != customer.currency) {
            throw CurrencyMismatchException(invoice.id, customer.id)
        }

        if (customer.balance < invoice.amount.value) {
            logger.debug("Customer '${customer.id}' doesn't have enough balance to make a payment")
            return false
            /*
            * Maybe send a notification to a customer.
            * */
        }

        return true
    }
}