/*
    Implements the data access layer (DAL).
    The data access layer generates and executes requests to the database.

    See the `mappings` module for the conversions between database rows and Kotlin objects.
 */

package io.pleo.antaeus.data

import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.math.BigDecimal

class AntaeusDal(private val db: Database) {
    suspend fun fetchInvoice(id: Int): Invoice? {
        // transaction(db) runs the internal query as a new database transaction.
        return newSuspendedTransaction(Dispatchers.IO, db) {
            // Returns the first invoice with matching id.
            InvoiceTable
                .select { InvoiceTable.id.eq(id) }
                .firstOrNull()
                ?.toInvoice()
        }
    }

    suspend fun fetchInvoices(): List<Invoice> {
        return newSuspendedTransaction(Dispatchers.IO, db) {
            InvoiceTable
                .selectAll()
                .map { it.toInvoice() }
        }
    }

    suspend fun fetchInvoicesByStatus(invoiceStatus: InvoiceStatus): List<Invoice> {
        return newSuspendedTransaction(Dispatchers.IO, db) {
            InvoiceTable
                .select { InvoiceTable.status.eq(invoiceStatus.name) }
                .map { it.toInvoice() }
        }
    }

    suspend fun createInvoice(amount: Money, customer: Customer, status: InvoiceStatus = InvoiceStatus.PENDING): Invoice? {
        val id = newSuspendedTransaction(Dispatchers.IO, db) {
            // Insert the invoice and returns its new id.
            InvoiceTable
                .insert {
                    it[this.value] = amount.value
                    it[this.currency] = amount.currency.toString()
                    it[this.status] = status.toString()
                    it[this.customerId] = customer.id
                } get InvoiceTable.id
        }

        return fetchInvoice(id)
    }

    suspend fun fetchCustomer(id: Int): Customer? {
        return newSuspendedTransaction(Dispatchers.IO, db) {
            CustomerTable
                .select { CustomerTable.id.eq(id) }
                .firstOrNull()
                ?.toCustomer()
        }
    }

    suspend fun fetchCustomers(): List<Customer> {
        return newSuspendedTransaction(Dispatchers.IO, db) {
            CustomerTable
                .selectAll()
                .map { it.toCustomer() }
        }
    }

    suspend fun createCustomer(currency: Currency, balance: BigDecimal): Customer? {
        val id = newSuspendedTransaction(Dispatchers.IO, db) {
            // Insert the customer and return its new id.
            CustomerTable.insert {
                it[this.currency] = currency.toString()
                it[this.balance] = balance
            } get CustomerTable.id
        }

        return fetchCustomer(id)
    }

    suspend fun makePayment(invoiceId: Int, customerId: Int, subtractedBalanceValue: BigDecimal) {
        newSuspendedTransaction(Dispatchers.IO, db) {
            subtractCustomerBalance(customerId, subtractedBalanceValue)
            updateInvoiceToBePaid(invoiceId)
        }
    }

    private fun subtractCustomerBalance(customerId: Int, subtractedBalanceValue: BigDecimal) {
        CustomerTable
            .update(
                where = { CustomerTable.id.eq(customerId) }
            ) {
                with(SqlExpressionBuilder) {
                    it.update(balance, balance - subtractedBalanceValue)
                }
            }
    }

    private fun updateInvoiceToBePaid(invoiceId: Int) {
        InvoiceTable
            .update(
                where = { InvoiceTable.id.eq(invoiceId) }
            ) {
                it[status] = InvoiceStatus.PAID.toString()
            }
    }
}
