package io.pleo.antaeus.data

import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import io.pleo.antaeus.utils.InvoiceTable
import io.pleo.antaeus.utils.toInvoice
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.LocalDateTime

class InvoiceDal(private val database: Database) {
    suspend fun fetchInvoice(id: Int): Invoice? {
        // transaction(db) runs the internal query as a new database transaction.
        return newSuspendedTransaction(Dispatchers.IO, database) {
            // Returns the first invoice with matching id.
            InvoiceTable
                .select { InvoiceTable.id.eq(id) }
                .firstOrNull()
                ?.toInvoice()
        }
    }

    suspend fun fetchInvoices(): List<Invoice> {
        return newSuspendedTransaction(Dispatchers.IO, database) {
            InvoiceTable
                .selectAll()
                .map { it.toInvoice() }
        }
    }

    suspend fun fetchInvoicesByStatus(
        invoiceStatus: InvoiceStatus, startPaymentDate: String, endPaymentDate: String): List<Invoice> {
        return newSuspendedTransaction(Dispatchers.IO, database) {
            InvoiceTable
                .select {
                    InvoiceTable.status.eq(invoiceStatus.name) and InvoiceTable.created.between(startPaymentDate, endPaymentDate)
                }
                .map { it.toInvoice() }
        }
    }

    suspend fun createInvoice(amount: Money, customer: Customer, status: InvoiceStatus = InvoiceStatus.PENDING): Invoice? {
        val id = newSuspendedTransaction(Dispatchers.IO, database) {
            // Insert the invoice and returns its new id.
            InvoiceTable
                .insert {
                    it[this.value] = amount.value
                    it[this.currency] = amount.currency.toString()
                    it[this.status] = status.toString()
                    it[this.customerId] = customer.id
                    it[this.created] = LocalDateTime.now().toString()
                } get InvoiceTable.id
        }

        return fetchInvoice(id)
    }
}
