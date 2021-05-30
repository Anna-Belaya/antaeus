package io.pleo.antaeus.core.factories

import io.pleo.antaeus.core.exceptions.PaymentProviderException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.external.PaymentProviderImpl
import io.pleo.antaeus.core.services.CustomerService

class PaymentProviderFactory {
    companion object {
        fun getPaymentProvider(paymentType: String, customerService: CustomerService): PaymentProvider {
            if ("online" == paymentType) {
                return PaymentProviderImpl(customerService)
            }

            throw PaymentProviderException()
        }
    }
}
