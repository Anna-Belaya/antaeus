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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.sql.Connection
import kotlin.random.Random

class PaymentDalTest {

    private val tables = arrayOf(CustomerTable, InvoiceTable)

    private val db = Database.connect("jdbc:sqlite:/tmp/paymenttestdata.db", "org.sqlite.JDBC")
    private val customerDal = CustomerDal(db = db)
    private val invoiceDal = InvoiceDal(db = db)
    private val paymentDal = PaymentDal(db = db)

    private val expectedCurrency = Currency.values()[Random.nextInt(0, Currency.values().size)]

    @BeforeEach
    fun before() {
        runBlocking {
            TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
            transaction(db) {
                addLogger(StdOutSqlLogger)
                SchemaUtils.drop(*tables)
                SchemaUtils.create(*tables)
            }
        }
    }

    @Test
    fun `makePayment no matches`() = runBlocking {
        val expectedBalance = BigDecimal(Random.nextDouble(10.0, 500.0))
        val invoiceValue = BigDecimal(Random.nextDouble(10.0, 500.0))
        val expectedCustomer = customerDal.createCustomer(expectedCurrency, expectedBalance)
        val expectedInvoice = expectedCustomer?.let {
            generateExpectedInvoice(it, invoiceValue, InvoiceStatus.PAID)
        }

        paymentDal.makePayment(11, 11, BigDecimal.valueOf(1))

        val actualCustomer = expectedCustomer?.id?.let { customerDal.fetchCustomer(it) }
        val actualInvoice = expectedInvoice?.id?.let { invoiceDal.fetchInvoice(it) }

        assertEquals(expectedCustomer, actualCustomer)
        assertEquals(expectedInvoice, actualInvoice)
    }

    @Test
    fun `makePayment`() = runBlocking {
        val startBalance = BigDecimal.valueOf(500)
        val invoiceValue = BigDecimal.valueOf(400)
        val expectedCustomer = customerDal.createCustomer(expectedCurrency, startBalance)
        val expectedInvoice = expectedCustomer?.let {
            generateExpectedInvoice(it, invoiceValue, InvoiceStatus.PENDING)
        }
        expectedInvoice?.id?.let { paymentDal.makePayment(expectedInvoice.id, it, expectedInvoice.amount.value) }

        val actualCustomer = expectedCustomer?.id?.let { customerDal.fetchCustomer(it) }
        val actualInvoice = expectedInvoice?.id?.let { invoiceDal.fetchInvoice(it) }

        assertEquals(startBalance - invoiceValue, actualCustomer?.balance)
        assertEquals(InvoiceStatus.PAID, actualInvoice?.status)
    }

    private suspend fun generateExpectedInvoice(customer: Customer, value: BigDecimal, status: InvoiceStatus): Invoice? {
        return invoiceDal.createInvoice(
            amount = Money(
                value = value,
                currency = expectedCurrency
            ),
            customer = customer,
            status = status
        )
    }
}
