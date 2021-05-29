package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.exceptions.SideUpdateException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService,
    private val paymentService: PaymentService
) {
    fun chargeInvoices() = runBlocking {
        val startPaymentDate = LocalDateTime.now()
            .minusMonths(1)
            .withDayOfMonth(1)
            .withHour(0)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
            .toString()
        val endPaymentDate = LocalDateTime.now()
            .withDayOfMonth(1)
            .withHour(0)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
            .toString()
        val pendingInvoices = invoiceService.fetchAllByStatus(InvoiceStatus.PENDING, startPaymentDate, endPaymentDate)

        pendingInvoices.groupBy { it.customerId }.entries.parallelStream().forEach {
            launch {
                it.value.sortedBy { it.created }.forEach {
                    chargeInvoice(it)
                }
            }
        }
    }

    private fun chargeInvoice(invoice: Invoice) = runBlocking {
        try {
            if (paymentProvider.charge(invoice)) {
                paymentService.makePayment(invoice.id, invoice.customerId, invoice.amount.value)
                logger.debug("Invoice with ${invoice.id} has been paid")
            }
        } catch (e: SideUpdateException) {
            logger.error(e.message)
        } catch (e: CustomerNotFoundException) {
            logger.error(e.message)
            /*
            * I would like these logs to be analysed
            * or stored in database for further (manually/automatic) investigation.
            * */
        } catch (e: CurrencyMismatchException) {
            logger.error(e.message)
            /*
            * As for simple version this situation could be just logged.
            * Currency transformation could be performed to allow user making a payment.
            * */
        } catch (e: NetworkException) {
            logger.error("Invoice ${invoice.id} couldn't be payed due to some network issues")
            /*
            * No payment is performed. Invoice charging should be retried.
            * */
        }
    }
}
