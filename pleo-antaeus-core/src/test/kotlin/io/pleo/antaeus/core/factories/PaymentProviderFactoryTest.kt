package io.pleo.antaeus.core.factories

import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.PaymentProviderException
import io.pleo.antaeus.core.external.PaymentProviderImpl
import io.pleo.antaeus.core.factories.PaymentProviderFactory.Companion.getPaymentProvider
import io.pleo.antaeus.core.services.CustomerService
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PaymentProviderFactoryTest {
    private val customerService = mockk<CustomerService>()

    @Test
    fun `getPaymentProvider online`() {
        val actualClassType = getPaymentProvider("online", customerService).javaClass
        assertTrue((PaymentProviderImpl::class.java).isAssignableFrom(actualClassType))
    }

    @Test
    fun `getPaymentProvider throws PaymentProviderException`() {
        assertThrows<PaymentProviderException> {
            getPaymentProvider("unknown", customerService).javaClass
        }
    }
}