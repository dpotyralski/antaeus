/*
    Defines the main() entry point of the app.
    Configures the database and sets up the REST web service.
 */

@file:JvmName("AntaeusApp")

package io.pleo.antaeus.app

import com.github.kagkarlsson.scheduler.SchedulerClient
import getPaymentProvider
import io.pleo.antaeus.core.infrastructure.DateTimeProvider
import io.pleo.antaeus.core.services.BillingScheduler
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.core.services.PaymentChargeTask
import io.pleo.antaeus.core.services.SchedulerConfiguration
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.data.CustomerTable
import io.pleo.antaeus.data.InvoiceTable
import io.pleo.antaeus.data.ScheduledTasks
import io.pleo.antaeus.rest.AntaeusRest
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.sqlite.SQLiteDataSource
import setupInitialData
import java.io.File
import java.sql.Connection
import java.time.Clock

fun main() {
    // The tables to create in the database.
    val tables = arrayOf(InvoiceTable, CustomerTable, ScheduledTasks)

    val dbFile: File = File.createTempFile("antaeus-db", ".sqlite")

    val dataSource = SQLiteDataSource()
    dataSource.url = "jdbc:sqlite:${dbFile.absolutePath}"

    // Connect to the database and create the needed tables. Drop any existing data.
    val db = Database
        .connect(dataSource)
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
    val dal = AntaeusDal(db = db)

    // Insert example data in the database.
    setupInitialData(dal = dal)

    // Get third parties
    val paymentProvider = getPaymentProvider()
    val dateTimeProvider = DateTimeProvider(Clock.systemDefaultZone())

    // Create core services
    val invoiceService = InvoiceService(dal = dal)
    val customerService = CustomerService(dal = dal)

    val schedulerClient: SchedulerClient = SchedulerClient.Builder.create(dataSource).build()
    // This is _your_ billing service to be included where you see fit
    val billingService = BillingService(
        dateTimeProvider = dateTimeProvider,
        schedulerClient = schedulerClient,
        invoiceService = invoiceService
    )
    val billingSchedulerTask = BillingScheduler(cronPattern = "33 04 18 * * *", billingService = billingService)
    val paymentChargeTask = PaymentChargeTask()

    val schedulerConfiguration = SchedulerConfiguration(
        dataSource = dataSource,
        onStartupScheduledTasks = listOf(billingSchedulerTask),
        onDemandSchedulerTasks = listOf(paymentChargeTask)
    )

    // Create REST web service
    AntaeusRest(
        invoiceService = invoiceService,
        customerService = customerService
    ).run()
}
