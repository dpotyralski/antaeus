package io.pleo.antaeus.core.services

import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.task.TaskInstance
import io.pleo.antaeus.core.infrastructure.DateTimeProvider
import io.pleo.antaeus.models.Invoice
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.temporal.ChronoUnit

class BillingService(
    private val dateTimeProvider: DateTimeProvider,
    private val schedulerClient: SchedulerClient,
    private val invoiceService: InvoiceService
) {

    companion object {
        val logger: Logger = LoggerFactory.getLogger(BillingService::class.java)
    }

    fun start() {
        logger.info("Invoice charging process started at: ${dateTimeProvider.now()}")
        var scheduleTime = dateTimeProvider.now()
        invoiceService.fetchPending()
            .forEach { invoice ->
                this.scheduleInvoiceCharge(scheduleTime, invoice)
                scheduleTime = scheduleTime.plus(10, ChronoUnit.SECONDS)
            }
    }

    private fun scheduleInvoiceCharge(scheduleTime: Instant, invoice: Invoice) {
        logger.info("Charging process for invoice $invoice scheduled on $scheduleTime")
        schedulerClient.schedule(
            TaskInstance(
                PaymentChargeTask.PAYMENT_CHARGE_TASK_NAME,
                invoice.id.toString(),
                invoice
            ), scheduleTime
        )
    }

}
