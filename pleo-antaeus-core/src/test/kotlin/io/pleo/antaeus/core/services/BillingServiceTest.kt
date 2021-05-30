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
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Test

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
        val expectedInvoiceList = generateInvoiceList(InvoiceStatus.PENDING)

        coEvery {
            invoiceService.fetchAllByStatus(eq(InvoiceStatus.PENDING), any(), any())
        } returns expectedInvoiceList

        coEvery {
            paymentProvider.isChargeable(any())
        } throws SideUpdateException(expectedInvoiceList[0].id)

        billingService.chargeInvoices()

        coVerify(exactly = 1) { invoiceService.fetchAllByStatus(eq(InvoiceStatus.PENDING), any(), any()) }
        coVerify(exactly = 9) { paymentProvider.isChargeable(any()) }
        coVerify(exactly = 0) { paymentService.makePayment(any(), any(), any()) }
    }

    @Test
    fun `chargeInvoices throws CustomerNotFoundException`() = runBlockingTest {
        val expectedInvoiceList = generateInvoiceList(InvoiceStatus.PENDING)

        coEvery {
            invoiceService.fetchAllByStatus(eq(InvoiceStatus.PENDING), any(), any())
        } returns expectedInvoiceList

        coEvery {
            paymentProvider.isChargeable(any())
        } throws CustomerNotFoundException(expectedInvoiceList[0].customerId)

        billingService.chargeInvoices()

        coVerify(exactly = 1) { invoiceService.fetchAllByStatus(eq(InvoiceStatus.PENDING), any(), any()) }
        coVerify(exactly = 9) { paymentProvider.isChargeable(any()) }
        coVerify(exactly = 0) { paymentService.makePayment(any(), any(), any()) }
    }

    @Test
    fun `chargeInvoices throws CurrencyMismatchException`() = runBlockingTest {
        val expectedInvoiceList = generateInvoiceList(InvoiceStatus.PENDING)

        coEvery {
            invoiceService.fetchAllByStatus(eq(InvoiceStatus.PENDING), any(), any())
        } returns expectedInvoiceList

        coEvery {
            paymentProvider.isChargeable(any())
        } throws CurrencyMismatchException(expectedInvoiceList[0].id, expectedInvoiceList[0].customerId)

        billingService.chargeInvoices()

        coVerify(exactly = 1) { invoiceService.fetchAllByStatus(eq(InvoiceStatus.PENDING), any(), any()) }
        coVerify(exactly = 9) { paymentProvider.isChargeable(any()) }
        coVerify(exactly = 0) { paymentService.makePayment(any(), any(), any()) }
    }

    @Test
    fun `chargeInvoices charge returns false`() = runBlockingTest {
        val expectedInvoiceList = generateInvoiceList(InvoiceStatus.PENDING)

        coEvery {
            invoiceService.fetchAllByStatus(eq(InvoiceStatus.PENDING), any(), any())
        } returns expectedInvoiceList

        coEvery {
            paymentProvider.isChargeable(any())
        } returns false

        billingService.chargeInvoices()

        coVerify(exactly = 1) { invoiceService.fetchAllByStatus(eq(InvoiceStatus.PENDING), any(), any()) }
        coVerify(exactly = 9) { paymentProvider.isChargeable(any()) }
        coVerify(exactly = 0) { paymentService.makePayment(any(), any(), any()) }
    }

    @Test
    fun `chargeInvoices charge returns true`() = runBlockingTest {
        val expectedInvoiceList = generateInvoiceList(InvoiceStatus.PENDING)

        coEvery {
            invoiceService.fetchAllByStatus(eq(InvoiceStatus.PENDING), any(), any())
        } returns expectedInvoiceList

        coEvery {
            paymentProvider.isChargeable(any())
        } returns true

        coEvery {
            paymentService.makePayment(any(), any(), any())
        } just Runs

        billingService.chargeInvoices()

        coVerify(exactly = 1) { invoiceService.fetchAllByStatus(eq(InvoiceStatus.PENDING), any(), any()) }
        coVerify(exactly = 9) { paymentProvider.isChargeable(any()) }
        coVerify(exactly = 9) { paymentService.makePayment(any(), any(), any()) }
    }

    @Test
    fun `chargeInvoices throws NetworkException`() = runBlockingTest {
        val expectedInvoiceList = generateInvoiceList(InvoiceStatus.PENDING)

        coEvery {
            invoiceService.fetchAllByStatus(eq(InvoiceStatus.PENDING), any(), any())
        } returns expectedInvoiceList

        coEvery {
            paymentProvider.isChargeable(any())
        } throws NetworkException()

        billingService.chargeInvoices()

        coVerify(exactly = 1) { invoiceService.fetchAllByStatus(eq(InvoiceStatus.PENDING), any(), any()) }
        coVerify(exactly = 9) { paymentProvider.isChargeable(any()) }
        coVerify(exactly = 0) { paymentService.makePayment(any(), any(), any()) }
    }
}
