package io.pleo.antaeus.core.services

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.data.CustomerDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Customer
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import kotlin.random.Random

class CustomerServiceTest {
    private val dal = mockk<CustomerDal>()

    private val customerService = CustomerService(dal = dal)

    @Test
    fun `fetchAll when there're no customers`() = runBlockingTest {
        val expectedCustomerList = emptyList<Customer>()

        coEvery {
            dal.fetchCustomers()
        } returns expectedCustomerList

        assertEquals(expectedCustomerList, customerService.fetchAll())

        coVerify(exactly = 1) { dal.fetchCustomers() }
    }

    @Test
    fun `fetchAll returns customer list`() = runBlockingTest {
        val expectedCustomerList = (1..10).mapNotNull {
            generateCustomer(it)
        }

        coEvery {
            dal.fetchCustomers()
        } returns expectedCustomerList

        assertEquals(expectedCustomerList, customerService.fetchAll())

        coVerify(exactly = 1) { dal.fetchCustomers() }
    }

    @Test
    fun `fetch will throw if customer is not found`() {
        val customerId = 1

        coEvery {
            dal.fetchCustomer(customerId)
        } returns null

        assertThrows<CustomerNotFoundException> {
            runBlockingTest {
                customerService.fetch(customerId)
            }
        }

        coVerify(exactly = 1) { dal.fetchCustomer(customerId) }
    }

    @Test
    fun `fetch by id returns customer`() = runBlockingTest {
        val customerId = 1
        val expectedCustomer = generateCustomer(customerId)

        coEvery {
            dal.fetchCustomer(customerId)
        } returns expectedCustomer

        assertEquals(expectedCustomer, customerService.fetch(customerId))

        coVerify(exactly = 1) { dal.fetchCustomer(customerId) }
    }

    private fun generateCustomer(id: Int): Customer {
        return Customer(
            id = id,
            currency = Currency.values()[Random.nextInt(0, Currency.values().size)],
            balance = BigDecimal(Random.nextDouble(10.0, 500.0))
        )
    }
}
