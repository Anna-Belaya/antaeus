/*
    Defines the main() entry point of the app.
    Configures the database and sets up the REST web service.
 */

@file:JvmName("AntaeusApp")

package io.pleo.antaeus.app

import getPaymentProvider
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.data.PaymentDal
import io.pleo.antaeus.data.CustomerDal
import io.pleo.antaeus.data.CustomerTable
import io.pleo.antaeus.data.InvoiceDal
import io.pleo.antaeus.data.InvoiceTable
import io.pleo.antaeus.rest.AntaeusRest
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
import setupInitialData
import java.io.File
import java.sql.Connection

object MonthlyScheduledPaymentJob : KronJob("monthly-scheduled-payment-job", "0 0 0 1 * ?")
object TestingJob : KronJob("testing-job", "0 * * ? * *")

fun main() {
    // The tables to create in the database.
    val tables = arrayOf(InvoiceTable, CustomerTable)

    val dbFile: File = File.createTempFile("antaeus-db", ".sqlite")
    // Connect to the database and create the needed tables. Drop any existing data.
    val db = Database
        .connect(url = "jdbc:sqlite:${dbFile.absolutePath}",
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

    // Set up data access layer.
    val paymentDal = PaymentDal(db = db)
    val invoiceDal = InvoiceDal(db = db)
    val customerDal = CustomerDal(db = db)

    // Insert example data in the database.
    runBlocking {
        setupInitialData(invoiceDal = invoiceDal, customerDal = customerDal)
    }

    // Create core services
    val invoiceService = InvoiceService(dal = invoiceDal)
    val customerService = CustomerService(dal = customerDal)

    // Get third parties
    val paymentProvider = getPaymentProvider(customerService)

    // This is _your_ billing service to be included where you see fit
    val billingService = BillingService(
        paymentProvider = paymentProvider,
        invoiceService = invoiceService,
        paymentDal = paymentDal
    )

    /*// InMem for testing purpose only
    val kjob = kjob(InMem) {
        extension(KronModule)
    }.start()

//    kjob(Kron).kron(MonthlyScheduledPaymentJob) {
    kjob(Kron).kron(TestingJob) {
        executionType = JobExecutionType.NON_BLOCKING
        maxRetries = 3
        execute {
            billingService.chargeInvoices()
        }.onError {
            logger.error("Job has failed")
            *//*
            * A email could be sent.
            * *//*
        }.onComplete {
            logger.info("Job has finished")
            *//*
            * A email could be sent.
            * *//*
        }
    }*/

    // Create REST web service
    AntaeusRest(
        invoiceService = invoiceService,
        customerService = customerService,
        //just for testing
        billingService = billingService
    ).run()
}
