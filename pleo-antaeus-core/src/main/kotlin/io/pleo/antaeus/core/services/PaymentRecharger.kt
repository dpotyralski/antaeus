package io.pleo.antaeus.core.services

import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.task.TaskInstance
import io.pleo.antaeus.core.infrastructure.DateTimeProvider
import io.pleo.antaeus.core.services.PaymentChargeTask.Companion.PAYMENT_CHARGE_TASK_NAME
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class PaymentRecharger(
    private val dateTimeProvider: DateTimeProvider,
    private val schedulerClient: SchedulerClient
) {

    fun recharge(invoiceCharge: InvoiceCharge) {
        val newExecutionTime = dateTimeProvider.now().plusSeconds(1)
        logger.info("Payment for invoice ${invoiceCharge.invoice} rescheduled for $newExecutionTime")
        invoiceCharge.increaseRetryCounter()
        schedulerClient.schedule(
            TaskInstance(
                PAYMENT_CHARGE_TASK_NAME,
                "invoice-${invoiceCharge.invoice.id}-reschedule-attempt-${invoiceCharge.retryCounter}",
                invoiceCharge
            ), newExecutionTime
        )
    }

}
