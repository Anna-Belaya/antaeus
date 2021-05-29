package io.pleo.antaeus.data

import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Customer
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
import kotlin.random.Random

class CustomerDalTest {

    private val customerTable = CustomerTable

    private val db = Database.connect("jdbc:sqlite:/tmp/customertestdata.db", "org.sqlite.JDBC")
    private val customerDal = CustomerDal(db = db)

    @BeforeEach
    fun before() {
        runBlocking {
            TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
            transaction(db) {
                addLogger(StdOutSqlLogger)
                SchemaUtils.drop(customerTable)
                SchemaUtils.create(customerTable)
            }
        }
    }

    @Test
    fun `fetchCustomer when there's no customers with such id`() = runBlocking {
        populateData()

        val nonExistedId = 1000
        val customer = customerDal.fetchCustomer(nonExistedId)

        assertNull(customer)
    }

    @Test
    fun `fetchCustomer by id returns customer`() = runBlocking {
        populateData()

        val expectedCurrency = Currency.values()[Random.nextInt(0, Currency.values().size)]
        val expectedBalance = BigDecimal(Random.nextDouble(10.0, 500.0))
        val expectedCustomer = customerDal.createCustomer(expectedCurrency, expectedBalance)

        val actualCustomer = expectedCustomer?.id?.let { customerDal.fetchCustomer(it) }

        assertEquals(expectedCustomer, actualCustomer)
    }

    @Test
    fun `fetchCustomers when there're no customers`() = runBlocking {
        assertEquals(emptyList<Customer>(), customerDal.fetchCustomers())
    }

    @Test
    fun `fetchCustomers returns customer list`() = runBlocking {
        val expectedCustomerList = populateData()

        val actualCustomerList = customerDal.fetchCustomers()

        assertEquals(expectedCustomerList.size, actualCustomerList.size)
        assertEquals(expectedCustomerList, actualCustomerList)
    }

    @Test
    fun `createCustomer`() = runBlocking {
        val expectedCurrency = Currency.values()[Random.nextInt(0, Currency.values().size)]
        val expectedBalance = BigDecimal(Random.nextDouble(10.0, 500.0))

        val actualCustomer = customerDal.createCustomer(expectedCurrency, expectedBalance)

        assertEquals(1, actualCustomer?.id)
    }

    private suspend fun populateData(): List<Customer> {
        return (1..10).mapNotNull {
            customerDal.createCustomer(
                currency = Currency.values()[Random.nextInt(0, Currency.values().size)],
                balance = BigDecimal(Random.nextDouble(10.0, 500.0))
            )
        }
    }
}
