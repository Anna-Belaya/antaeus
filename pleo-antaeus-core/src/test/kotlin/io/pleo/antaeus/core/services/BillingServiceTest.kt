package io.pleo.antaeus.core.services

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.exceptions.SideUpdateException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.random.Random

class BillingServiceTest {
    private val paymentProvider = mockk<PaymentProvider>()
    private val invoiceService = mockk<InvoiceService>()
    private val paymentService = mockk<PaymentService>()

    private val billingService = BillingService(paymentProvider, invoiceService, paymentService)

    @Test
    fun `chargeInvoices when there're no invoices`() = runBlockingTest {
        val expectedInvoiceList = emptyList<Invoice>()

        coEvery {
            invoiceService.fetchAllByStatus(eq(InvoiceStatus.PENDING), any(), any())
        } returns expectedInvoiceList

        billingService.chargeInvoices()

        coVerify(exactly = 1) { invoiceService.fetchAllByStatus(eq(InvoiceStatus.PENDING), any(), any()) }
        coVerify(exactly = 0) { paymentService.makePayment(any(), any(), any()) }
    }

    @Test
    fun `chargeInvoices throws SideUpdateException`() = runBlockingTest {
        val expectedInvoiceList = generateInvoiceList()

        coEvery {
            invoiceService.fetchAllByStatus(eq(InvoiceStatus.PENDING), any(), any())
        } returns expectedInvoiceList

        coEvery {
            paymentProvider.charge(any())
        } throws SideUpdateException(expectedInvoiceList[0].id)

        billingService.chargeInvoices()

        coVerify(exactly = 1) { invoiceService.fetchAllByStatus(eq(InvoiceStatus.PENDING), any(), any()) }
        coVerify(exactly = 9) { paymentProvider.charge(any()) }
        coVerify(exactly = 0) { paymentService.makePayment(any(), any(), any()) }
    }

    @Test
    fun `chargeInvoices throws CustomerNotFoundException`() = runBlockingTest {
        val expectedInvoiceList = generateInvoiceList()

        coEvery {
            invoiceService.fetchAllByStatus(eq(InvoiceStatus.PENDING), any(), any())
        } returns expectedInvoiceList

        coEvery {
            paymentProvider.charge(any())
        } throws CustomerNotFoundException(expectedInvoiceList[0].customerId)

        billingService.chargeInvoices()

        coVerify(exactly = 1) { invoiceService.fetchAllByStatus(eq(InvoiceStatus.PENDING), any(), any()) }
        coVerify(exactly = 9) { paymentProvider.charge(any()) }
        coVerify(exactly = 0) { paymentService.makePayment(any(), any(), any()) }
    }

    @Test
    fun `chargeInvoices throws CurrencyMismatchException`() = runBlockingTest {
        val expectedInvoiceList = generateInvoiceList()

        coEvery {
            invoiceService.fetchAllByStatus(eq(InvoiceStatus.PENDING), any(), any())
        } returns expectedInvoiceList

        coEvery {
            paymentProvider.charge(any())
        } throws CurrencyMismatchException(expectedInvoiceList[0].id, expectedInvoiceList[0].customerId)

        billingService.chargeInvoices()

        coVerify(exactly = 1) { invoiceService.fetchAllByStatus(eq(InvoiceStatus.PENDING), any(), any()) }
        coVerify(exactly = 9) { paymentProvider.charge(any()) }
        coVerify(exactly = 0) { paymentService.makePayment(any(), any(), any()) }
    }

    @Test
    fun `chargeInvoices charge returns false`() = runBlockingTest {
        val expectedInvoiceList = generateInvoiceList()

        coEvery {
            invoiceService.fetchAllByStatus(eq(InvoiceStatus.PENDING), any(), any())
        } returns expectedInvoiceList

        coEvery {
            paymentProvider.charge(any())
        } returns false

        billingService.chargeInvoices()

        coVerify(exactly = 1) { invoiceService.fetchAllByStatus(eq(InvoiceStatus.PENDING), any(), any()) }
        coVerify(exactly = 9) { paymentProvider.charge(any()) }
        coVerify(exactly = 0) { paymentService.makePayment(any(), any(), any()) }
    }

    @Test
    fun `chargeInvoices charge returns true`() = runBlockingTest {
        val expectedInvoiceList = generateInvoiceList()

        coEvery {
            invoiceService.fetchAllByStatus(eq(InvoiceStatus.PENDING), any(), any())
        } returns expectedInvoiceList

        coEvery {
            paymentProvider.charge(any())
        } returns true

        coEvery {
            paymentService.makePayment(any(), any(), any())
        } just Runs

        billingService.chargeInvoices()

        coVerify(exactly = 1) { invoiceService.fetchAllByStatus(eq(InvoiceStatus.PENDING), any(), any()) }
        coVerify(exactly = 9) { paymentProvider.charge(any()) }
        coVerify(exactly = 9) { paymentService.makePayment(any(), any(), any()) }
    }

    @Test
    fun `chargeInvoices throws NetworkException`() = runBlockingTest {
        val expectedInvoiceList = generateInvoiceList()

        coEvery {
            invoiceService.fetchAllByStatus(eq(InvoiceStatus.PENDING), any(), any())
        } returns expectedInvoiceList

        coEvery {
            paymentProvider.charge(any())
        } throws NetworkException()

        billingService.chargeInvoices()

        coVerify(exactly = 1) { invoiceService.fetchAllByStatus(eq(InvoiceStatus.PENDING), any(), any()) }
        coVerify(exactly = 9) { paymentProvider.charge(any()) }
        coVerify(exactly = 0) { paymentService.makePayment(any(), any(), any()) }
    }

    private fun generateInvoiceList(): MutableList<Invoice> {
        val expectedInvoiceList = mutableListOf<Invoice>()
        val customers = (1..3).mapNotNull {
            Customer(
                id = it,
                currency = Currency.values()[Random.nextInt(0, Currency.values().size)],
                balance = BigDecimal(Random.nextDouble(10.0, 500.0))
            )
        }

        customers.flatMap { customer ->
            (1..3).mapNotNull {
                val invoice = Invoice(
                    id = it,
                    customerId = customer.id,
                    amount = Money(
                        value = BigDecimal(Random.nextDouble(10.0, 500.0)),
                        currency = customer.currency
                    ),
                    status = InvoiceStatus.PENDING,
                    created = LocalDateTime.now().toString()
                )
                expectedInvoiceList.add(invoice)
            }
        }

        return expectedInvoiceList
    }
}
