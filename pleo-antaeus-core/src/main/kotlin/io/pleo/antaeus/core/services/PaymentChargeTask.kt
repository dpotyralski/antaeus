package io.pleo.antaeus.core.services

import com.github.kagkarlsson.scheduler.task.ExecutionContext
import com.github.kagkarlsson.scheduler.task.TaskInstance
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant

class PaymentChargeTask(
    private val invoiceService: InvoiceService,
    private val paymentProvider: PaymentProvider
) : OnDemandSchedulerTask<Invoice> {

    companion object {
        val logger: Logger = LoggerFactory.getLogger(PaymentChargeTask::class.java)
        const val PAYMENT_CHARGE_TASK_NAME = "paymentCharger"
    }

    private val chargeRechargeTask: OneTimeTask<Invoice> =
        Tasks.oneTime(PAYMENT_CHARGE_TASK_NAME, Invoice::class.java)
            .execute { taskInstance: TaskInstance<Invoice>, _: ExecutionContext ->
                val invoice = taskInstance.data
                logger.info("Charging invoice: $invoice = at ${Instant.now()}")
                if (paymentProvider.charge(invoice)) {
                    invoiceService.markInvoiceAsPaid(invoice)
                    logger.info("Invoice with id: ${invoice.id} was charged successfully")
                } else {
                    //todo recharge payment ?
                }
            }

    override fun getTask(): OneTimeTask<Invoice> {
        return chargeRechargeTask;
    }

}