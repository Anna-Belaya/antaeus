/*
    Implements the data access layer (DAL).
    The data access layer generates and executes requests to the database.

    See the `mappings` module for the conversions between database rows and Kotlin objects.
 */

package io.pleo.antaeus.data

import io.pleo.antaeus.models.InvoiceStatus
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.math.BigDecimal


class PaymentDal(private val db: Database) {
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
