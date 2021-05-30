package io.pleo.antaeus.core.exetrnal

import io.mockk.coEvery
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.SideUpdateException
import io.pleo.antaeus.core.external.PaymentProviderImpl
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.generateInvoiceList
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.random.Random

class PaymentProviderImplTest {
    private val customerService = mockk<CustomerService>()

    private val paymentProvider = PaymentProviderImpl(customerService)

    @Test
    fun `isChargeable throws SideUpdateException`() {
        val expectedInvoice = generateInvoiceList(InvoiceStatus.PAID)[0]

        assertThrows<SideUpdateException> {
            runBlockingTest {
                paymentProvider.isChargeable(expectedInvoice)
            }
        }
    }

    @Test
    fun `isChargeable throws CustomerNotFoundException`() {
        val expectedInvoice = generateInvoiceList(InvoiceStatus.PENDING)[0]
        val expectedCustomerId = expectedInvoice.customerId

        coEvery {
            customerService.fetch(expectedCustomerId)
        } throws CustomerNotFoundException(expectedCustomerId)

        assertThrows<CustomerNotFoundException> {
            runBlockingTest {
                paymentProvider.isChargeable(expectedInvoice)
            }
        }
    }

    @Test
    fun `isChargeable throws CurrencyMismatchException`() {
        val expectedInvoice = Invoice(
            id = 1,
            customerId = 1,
            amount = Money(
                value = BigDecimal(Random.nextDouble(10.0, 500.0)),
                currency = Currency.USD
            ),
            status = InvoiceStatus.PENDING,
            created = LocalDateTime.now().toString()
        )
        val expectedCustomerId = expectedInvoice.customerId
        val expectedCustomer = Customer(
            id = expectedCustomerId,
            currency = Currency.EUR,
            balance = BigDecimal(Random.nextDouble(10.0, 500.0))
        )

        coEvery {
            customerService.fetch(expectedCustomerId)
        } returns expectedCustomer

        assertThrows<CurrencyMismatchException> {
            runBlockingTest {
                paymentProvider.isChargeable(expectedInvoice)
            }
        }
    }

    @Test
    fun `isChargeable returns false`() = runBlockingTest {
        val expectedInvoice = Invoice(
            id = 1,
            customerId = 1,
            amount = Money(
                value = BigDecimal.valueOf(200),
                currency = Currency.USD
            ),
            status = InvoiceStatus.PENDING,
            created = LocalDateTime.now().toString()
        )
        val expectedCustomerId = expectedInvoice.customerId
        val expectedCustomer = Customer(
            id = expectedCustomerId,
            currency = Currency.USD,
            balance = BigDecimal.valueOf(100)
        )

        coEvery {
            customerService.fetch(expectedCustomerId)
        } returns expectedCustomer

        assertFalse(paymentProvider.isChargeable(expectedInvoice))
    }

    @Test
    fun `isChargeable returns true`() = runBlockingTest {
        val expectedInvoice = Invoice(
            id = 1,
            customerId = 1,
            amount = Money(
                value = BigDecimal.valueOf(200),
                currency = Currency.USD
            ),
            status = InvoiceStatus.PENDING,
            created = LocalDateTime.now().toString()
        )
        val expectedCustomerId = expectedInvoice.customerId
        val expectedCustomer = Customer(
            id = expectedCustomerId,
            currency = Currency.USD,
            balance = BigDecimal.valueOf(200)
        )

        coEvery {
            customerService.fetch(expectedCustomerId)
        } returns expectedCustomer

        assertTrue(paymentProvider.isChargeable(expectedInvoice))
    }
}
