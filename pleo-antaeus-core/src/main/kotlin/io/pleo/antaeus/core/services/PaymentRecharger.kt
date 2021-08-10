package io.pleo.antaeus.core.services

import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.task.TaskInstance
import io.pleo.antaeus.core.infrastructure.DateTimeProvider
import io.pleo.antaeus.core.services.PaymentChargeTask.Companion.PAYMENT_CHARGE_TASK_NAME
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PaymentRecharger(
    private val dateTimeProvider: DateTimeProvider,
    private val schedulerClient: SchedulerClient
) {

    companion object {
        val logger: Logger = LoggerFactory.getLogger(PaymentRecharger::class.java)
    }

    fun recharge(invoiceCharge: InvoiceCharge) {
        val newExecutionTime = dateTimeProvider.now().plusSeconds(10)
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
