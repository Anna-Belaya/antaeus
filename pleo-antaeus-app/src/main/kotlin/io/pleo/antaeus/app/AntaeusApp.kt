/*
    Defines the main() entry point of the app.
    Configures the database and sets up the REST web service.
 */

@file:JvmName("AntaeusApp")

package io.pleo.antaeus.app

import io.pleo.antaeus.core.factories.PaymentProviderFactory
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.core.services.PaymentService
import io.pleo.antaeus.data.CustomerDal
import io.pleo.antaeus.data.InvoiceDal
import io.pleo.antaeus.data.PaymentDal
import io.pleo.antaeus.rest.AntaeusRest
import io.pleo.antaeus.utils.CustomerTable
import io.pleo.antaeus.utils.InvoiceTable
import io.pleo.antaeus.utils.setupInitialData
import it.justwrote.kjob.InMem
import it.justwrote.kjob.KronJob
import it.justwrote.kjob.job.JobExecutionType
import it.justwrote.kjob.kjob
import it.justwrote.kjob.kron.Kron
import it.justwrote.kjob.kron.KronModule
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.sql.Connection

object MonthlyScheduledPaymentJob : KronJob("monthly-scheduled-payment-job", "0 0 0 1 * ?")

fun main() {
    val database = initializeDatabase()

    // Set up data access layer.
    val paymentDal = PaymentDal(database = database)
    val invoiceDal = InvoiceDal(database = database)
    val customerDal = CustomerDal(database = database)

    // Insert example data in the database.
    runBlocking {
        setupInitialData(invoiceDal = invoiceDal, customerDal = customerDal)
    }

    // Create core services
    val paymentService = PaymentService(dal = paymentDal)
    val invoiceService = InvoiceService(dal = invoiceDal)
    val customerService = CustomerService(dal = customerDal)

    // Get third parties
    val paymentProvider = PaymentProviderFactory.getPaymentProvider("online", customerService)

    // This is _your_ billing service to be included where you see fit
    val billingService = BillingService(
        paymentProvider = paymentProvider,
        invoiceService = invoiceService,
        paymentService = paymentService
    )

    runCronJob(billingService)

    runWebService(invoiceService, customerService, billingService)
}

private fun initializeDatabase(): Database {
    // The tables to create in the database.
    val tables = arrayOf(InvoiceTable, CustomerTable)

    val databaseFile: File = File.createTempFile("antaeus-db", ".sqlite")

    // Connect to the database and create the needed tables. Drop any existing data.

    return Database
        .connect(url = "jdbc:sqlite:${databaseFile.absolutePath}",
            driver = "org.sqlite.JDBC",
            user = "root",
            password = "")
        .also {
            TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
            transaction(it) {
                addLogger(StdOutSqlLogger)
                // Drop all existing tables to ensure a clean slate on each run
                SchemaUtils.drop(*tables)
                // Create all tables
                SchemaUtils.create(*tables)
            }
        }
}

/*
An email could be sent after fail or success event.
 */
private fun runCronJob(billingService: BillingService) {
    // InMem for testing purpose only
    val kjob = kjob(InMem) {
        extension(KronModule)
    }.start()

    kjob(Kron).kron(MonthlyScheduledPaymentJob) {
        executionType = JobExecutionType.NON_BLOCKING
        maxRetries = 3
        execute {
            billingService.chargeInvoices()
        }.onError {
            logger.error("Job with jobId = '$jobId' has failed")
        }.onComplete {
            logger.info("Job with jobId = '$jobId' has finished")
        }
    }
}

private fun runWebService(
    invoiceService: InvoiceService, customerService: CustomerService, billingService: BillingService) {
    // Create REST web service
    AntaeusRest(
        invoiceService = invoiceService,
        customerService = customerService,
        //just for testing
        billingService = billingService
    ).run()
}
