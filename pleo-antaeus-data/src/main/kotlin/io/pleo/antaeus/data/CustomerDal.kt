package io.pleo.antaeus.data

import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.utils.CustomerTable
import io.pleo.antaeus.utils.toCustomer
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.math.BigDecimal

class CustomerDal(private val database: Database) {
    suspend fun fetchCustomer(id: Int): Customer? {
        return newSuspendedTransaction(Dispatchers.IO, database) {
            CustomerTable
                .select { CustomerTable.id.eq(id) }
                .firstOrNull()
                ?.toCustomer()
        }
    }

    suspend fun fetchCustomers(): List<Customer> {
        return newSuspendedTransaction(Dispatchers.IO, database) {
            CustomerTable
                .selectAll()
                .map { it.toCustomer() }
        }
    }

    suspend fun createCustomer(currency: Currency, balance: BigDecimal): Customer? {
        val id = newSuspendedTransaction(Dispatchers.IO, database) {
            // Insert the customer and return its new id.
            CustomerTable.insert {
                it[this.currency] = currency.toString()
                it[this.balance] = balance
            } get CustomerTable.id
        }

        return fetchCustomer(id)
    }
}
