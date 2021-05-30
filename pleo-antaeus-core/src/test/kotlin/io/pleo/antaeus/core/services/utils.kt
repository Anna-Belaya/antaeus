package io.pleo.antaeus.core.services

import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.random.Random

fun generateCustomer(id: Int): Customer {
    return Customer(
        id = id,
        currency = Currency.values()[Random.nextInt(0, Currency.values().size)],
        balance = BigDecimal(Random.nextDouble(10.0, 500.0))
    )
}

fun generateInvoiceList(invoiceStatus: InvoiceStatus): MutableList<Invoice> {
    val expectedInvoiceList = mutableListOf<Invoice>()
    val customers = (1..3).mapNotNull {
        generateCustomer(it)
    }

    customers.flatMap { customer ->
        (1..3).mapNotNull {
            val invoice = Invoice(
                id = it,
                customerId = customer.id,
                amount = Money(
                    value = BigDecimal(Random.nextDouble(10.0, 500.0)),
                    currency = customer.currency
                ),
                status = invoiceStatus,
                created = LocalDateTime.now().toString()
            )
            expectedInvoiceList.add(invoice)
        }
    }

    return expectedInvoiceList
}
