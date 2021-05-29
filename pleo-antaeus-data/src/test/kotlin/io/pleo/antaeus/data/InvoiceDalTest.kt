package io.pleo.antaeus.data

import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.sql.Connection
import java.time.LocalDateTime
import kotlin.random.Random

class InvoiceDalTest {

    private val invoiceTable = InvoiceTable

    private val db = Database.connect("jdbc:sqlite:/tmp/invoicetestdata.db", "org.sqlite.JDBC")
    private val invoiceDal = InvoiceDal(db = db)

    private val startPaymentDate = LocalDateTime.now()
        .minusDays(1)
        .toString()
    private val endPaymentDate = LocalDateTime.now()
        .plusDays(1)
        .toString()

    @BeforeEach
    fun before() {
        runBlocking {
            TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
            transaction(db) {
                addLogger(StdOutSqlLogger)
                SchemaUtils.drop(invoiceTable)
                SchemaUtils.create(invoiceTable)
            }
        }
    }

    @Test
    fun `fetchInvoice when there's no invoices with such id`() = runBlocking {
        populateData()

        val nonExistedId = 1000
        val invoice = invoiceDal.fetchInvoice(nonExistedId)

        assertNull(invoice)
    }

    @Test
    fun `fetchInvoice by id returns invoice`() = runBlocking {
        populateData()

        val expectedInvoice = generateExpectedInvoice()

        val actualInvoice = expectedInvoice?.id?.let { invoiceDal.fetchInvoice(it) }

        assertEquals(expectedInvoice, actualInvoice)
    }

    @Test
    fun `fetchInvoices when there're no invoices`() = runBlocking {
        assertEquals(emptyList<Customer>(), invoiceDal.fetchInvoices())
    }

    @Test
    fun `fetchInvoices returns invoice list`() = runBlocking {
        val expectedInvoiceList = populateData()

        val actualInvoiceList = invoiceDal.fetchInvoices()

        assertEquals(expectedInvoiceList.size, actualInvoiceList.size)
        assertEquals(expectedInvoiceList, actualInvoiceList)
    }

    @Test
    fun `fetchInvoicesByStatus when there're no invoices`() = runBlocking {
        generateExpectedInvoice()

        assertEquals(emptyList<Customer>(),
            invoiceDal.fetchInvoicesByStatus(InvoiceStatus.PENDING, startPaymentDate, endPaymentDate))
    }

    @Test
    fun `fetchInvoicesByStatus returns invoice list`() = runBlocking {
        val expectedInvoiceList = populateData().filter { invoice -> InvoiceStatus.PAID == invoice.status }

        val actualInvoiceList = invoiceDal.fetchInvoicesByStatus(InvoiceStatus.PAID, startPaymentDate, endPaymentDate)

        assertEquals(expectedInvoiceList, actualInvoiceList)
    }

    @Test
    fun `createInvoice`() = runBlocking {
        val actualInvoice = generateExpectedInvoice()

        assertEquals(1, actualInvoice?.id)
    }

    private suspend fun populateData(): List<Invoice> {
        val invoiceList = mutableListOf<Invoice>()
        val customers = (1..10).mapNotNull {
            generateCustomer(it)
        }

        customers.flatMap { customer ->
            (1..10).mapNotNull {
                val invoice = generateInvoice(customer.currency, customer)
                if (invoice != null) {
                    invoiceList.add(invoice)
                }
            }
        }

        return invoiceList
    }

    private suspend fun generateExpectedInvoice(): Invoice? {
        val expectedCurrency = Currency.values()[Random.nextInt(0, Currency.values().size)]
        val expectedCustomer = generateCustomer(1000)

        return generateInvoice(expectedCurrency, expectedCustomer)
    }

    private fun generateCustomer(customerId: Int): Customer {
        val expectedCurrency = Currency.values()[Random.nextInt(0, Currency.values().size)]
        val expectedBalance = BigDecimal(Random.nextDouble(10.0, 500.0))

        return Customer(customerId, expectedCurrency, expectedBalance)
    }

    private suspend fun generateInvoice(expectedCurrency: Currency, expectedCustomer: Customer): Invoice? {
        return invoiceDal.createInvoice(
            amount = Money(
                value = BigDecimal(Random.nextDouble(10.0, 500.0)),
                currency = expectedCurrency
            ),
            customer = expectedCustomer,
            status = InvoiceStatus.PAID
        )
    }
}
