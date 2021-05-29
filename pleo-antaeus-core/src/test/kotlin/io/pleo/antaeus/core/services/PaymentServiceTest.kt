package io.pleo.antaeus.core.services

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.data.PaymentDal
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class PaymentServiceTest {
    private val dal = mockk<PaymentDal>()

    private val paymentService = PaymentService(dal = dal)

    private val invoiceId = 1
    private val customerId = 1
    private val subtractedBalanceValue = BigDecimal.valueOf(1)

    @Test
    fun `makePayment throws NetworkException`() {
        coEvery {
            dal.makePayment(invoiceId, customerId, subtractedBalanceValue)
        } throws NetworkException()

        assertThrows<NetworkException> {
            runBlockingTest {
                paymentService.makePayment(invoiceId, customerId, subtractedBalanceValue)
            }
        }

        coVerify(exactly = 1) { dal.makePayment(invoiceId, customerId, subtractedBalanceValue) }
    }

    @Test
    fun `makePayment`() = runBlockingTest {
        coEvery {
            dal.makePayment(invoiceId, customerId, subtractedBalanceValue)
        } just Runs

        paymentService.makePayment(invoiceId, customerId, subtractedBalanceValue)

        coVerify(exactly = 1) { dal.makePayment(invoiceId, customerId, subtractedBalanceValue) }
    }
}
