package io.pleo.antaeus.core.services

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.InvoiceDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

class InvoiceServiceTest {
    private val dal = mockk<InvoiceDal>()

    private val invoiceService = InvoiceService(dal = dal)

    private val startPaymentDate = LocalDateTime.now()
        .minusDays(1)
        .toString()
    private val endPaymentDate = LocalDateTime.now()
        .plusDays(1)
        .toString()

    @Test
    fun `fetchAll when there're no invoices`() = runBlockingTest {
        val expectedInvoiceList = emptyList<Invoice>()

        coEvery {
            dal.fetchInvoices()
        } returns expectedInvoiceList

        assertEquals(expectedInvoiceList, invoiceService.fetchAll())

        coVerify(exactly = 1) { dal.fetchInvoices() }
    }

    @Test
    fun `fetchAll returns invoice list`() = runBlockingTest {
        val expectedInvoiceList = generateInvoiceList(InvoiceStatus.PAID)

        coEvery {
            dal.fetchInvoices()
        } returns expectedInvoiceList

        assertEquals(expectedInvoiceList, invoiceService.fetchAll())

        coVerify(exactly = 1) { dal.fetchInvoices() }
    }

    @Test
    fun `fetchAllByStatus when there're no invoices`() = runBlockingTest {
        val expectedInvoiceList = emptyList<Invoice>()

        coEvery {
            dal.fetchInvoicesByStatus(InvoiceStatus.PENDING, startPaymentDate, endPaymentDate)
        } returns expectedInvoiceList

        assertEquals(expectedInvoiceList,
            invoiceService.fetchAllByStatus(InvoiceStatus.PENDING, startPaymentDate, endPaymentDate))

        coVerify(exactly = 1) { dal.fetchInvoicesByStatus(InvoiceStatus.PENDING, startPaymentDate, endPaymentDate) }
    }

    @Test
    fun `fetchAllByStatus returns invoice list`() = runBlockingTest {
        val expectedInvoiceList = generateInvoiceList(InvoiceStatus.PAID)

        coEvery {
            dal.fetchInvoicesByStatus(InvoiceStatus.PAID, startPaymentDate, endPaymentDate)
        } returns expectedInvoiceList

        assertEquals(expectedInvoiceList,
            invoiceService.fetchAllByStatus(InvoiceStatus.PAID, startPaymentDate, endPaymentDate))

        coVerify(exactly = 1) { dal.fetchInvoicesByStatus(InvoiceStatus.PAID, startPaymentDate, endPaymentDate) }
    }

    @Test
    fun `fetch will throw if invoice is not found`() {
        val invoiceId = 1

        coEvery {
            dal.fetchInvoice(invoiceId)
        } returns null

        assertThrows<InvoiceNotFoundException> {
            runBlockingTest {
                invoiceService.fetch(invoiceId)
            }
        }

        coVerify(exactly = 1) { dal.fetchInvoice(invoiceId) }
    }

    @Test
    fun `fetch by id returns invoice`() = runBlockingTest {
        val expectedInvoice = generateInvoiceList(InvoiceStatus.PAID)[0]
        val invoiceId = expectedInvoice.id

        coEvery {
            dal.fetchInvoice(invoiceId)
        } returns expectedInvoice

        assertEquals(expectedInvoice, invoiceService.fetch(invoiceId))

        coVerify(exactly = 1) { dal.fetchInvoice(invoiceId) }
    }
}
