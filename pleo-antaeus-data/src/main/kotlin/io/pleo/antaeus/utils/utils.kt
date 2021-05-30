package io.pleo.antaeus.utils

import io.pleo.antaeus.data.CustomerDal
import io.pleo.antaeus.data.InvoiceDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import java.math.BigDecimal
import kotlin.random.Random

suspend fun setupCustomers(customerCount: Int, customerDal: CustomerDal): List<Customer> {
    return (1..customerCount).mapNotNull {
        customerDal.createCustomer(
            currency = Currency.values()[Random.nextInt(0, Currency.values().size)],
            balance = BigDecimal(Random.nextDouble(10.0, 500.0))
        )
    }
}

suspend fun setupInvoice(invoiceDal: InvoiceDal, expectedCurrency: Currency, expectedCustomer: Customer, index: Int):
    Invoice? {
    return invoiceDal.createInvoice(
        amount = Money(
            value = BigDecimal(Random.nextDouble(10.0, 500.0)),
            currency = expectedCurrency
        ),
        customer = expectedCustomer,
        status = if (index == 1) InvoiceStatus.PENDING else InvoiceStatus.PAID
    )
}

suspend fun setupInitialData(invoiceDal: InvoiceDal, customerDal: CustomerDal) {
    val customers = setupCustomers(100, customerDal)

    customers.forEach { customer ->
        (1..10).forEach {
            setupInvoice(invoiceDal, customer.currency, customer, it)
        }
    }
}
