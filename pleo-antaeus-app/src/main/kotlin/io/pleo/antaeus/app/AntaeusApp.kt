/*
    Defines the main() entry point of the app.
    Configures the database and sets up the REST web service.
 */

@file:JvmName("AntaeusApp")

package io.pleo.antaeus.app

import com.github.kagkarlsson.scheduler.SchedulerClient
import getPaymentCronConfiguration
import getPaymentProvider
import io.pleo.antaeus.core.infrastructure.DateTimeProvider
import io.pleo.antaeus.core.services.BillingScheduler
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.core.services.PaymentChargeFailureHandler
import io.pleo.antaeus.core.services.PaymentChargeTask
import io.pleo.antaeus.core.services.PaymentRecharger
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
import org.postgresql.ds.PGSimpleDataSource
import setupInitialData
import java.sql.Connection
import java.time.Clock

fun main() {
    // The tables to create in the database.
    val tables = arrayOf(InvoiceTable, CustomerTable, ScheduledTasks)

    val dataSource = PGSimpleDataSource()
    dataSource.setUrl("jdbc:postgresql://${System.getenv("DB_HOST")}:${System.getenv("DB_PORT")}/antaeus-db")
    dataSource.user = System.getenv("DB_USERNAME")
    dataSource.password = System.getenv("DB_PASSWORD")

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
    val billingSchedulerTask = BillingScheduler(cronPattern = getPaymentCronConfiguration(), billingService = billingService)

    val paymentRecharger = PaymentRecharger(
        dateTimeProvider = dateTimeProvider,
        schedulerClient = schedulerClient
    )

    val paymentChargeFailureHandler = PaymentChargeFailureHandler(
        dateTimeProvider = dateTimeProvider
    )

    val paymentChargeTask = PaymentChargeTask(
        invoiceService = invoiceService,
        paymentProvider = paymentProvider,
        paymentRecharger = paymentRecharger,
        failureHandler = paymentChargeFailureHandler
    )

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
