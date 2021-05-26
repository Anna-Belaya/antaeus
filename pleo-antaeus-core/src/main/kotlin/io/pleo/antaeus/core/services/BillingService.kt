package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.exceptions.SideUpdateException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.InvoiceStatus
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService,
    private val dal: AntaeusDal
) {
    fun chargeBills() = runBlocking() {
        val pendingInvoices = invoiceService.fetchAllByStatus(InvoiceStatus.PENDING)

        pendingInvoices.parallelStream().forEach {
            launch {
                try {
                    if (paymentProvider.charge(it)) {
                        dal.makePayment(it.id, it.customerId, it.amount.value)
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
                    logger.error("Invoice ${it.id} couldn't be payed due to some network issues")
                    /*
                    * No payment is performed. Invoice charging should be retried.
                    * */
                }
            }
        }
    }
}
