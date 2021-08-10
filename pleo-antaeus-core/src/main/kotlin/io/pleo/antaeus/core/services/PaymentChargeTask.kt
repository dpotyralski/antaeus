package io.pleo.antaeus.core.services

import com.github.kagkarlsson.scheduler.task.ExecutionContext
import com.github.kagkarlsson.scheduler.task.TaskInstance
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import io.pleo.antaeus.core.external.PaymentProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant

class PaymentChargeTask(
    private val invoiceService: InvoiceService,
    private val paymentProvider: PaymentProvider,
    private val paymentRecharger: PaymentRecharger
) : OnDemandSchedulerTask<InvoiceCharge> {

    companion object {
        val logger: Logger = LoggerFactory.getLogger(PaymentChargeTask::class.java)
        const val PAYMENT_CHARGE_TASK_NAME = "paymentCharger"
    }

    private val chargeRechargeTask: OneTimeTask<InvoiceCharge> =
        Tasks.oneTime(PAYMENT_CHARGE_TASK_NAME, InvoiceCharge::class.java)
            .execute { taskInstance: TaskInstance<InvoiceCharge>, _: ExecutionContext ->
                val invoiceCharge = taskInstance.data
                logger.info("Charging invoice: ${invoiceCharge.invoice} = at ${Instant.now()}")
                if (paymentProvider.charge(invoiceCharge.invoice)) {
                    invoiceService.markInvoiceAsPaid(invoiceCharge.invoice)
                    logger.info("Invoice with id: ${invoiceCharge.invoice.id} was charged successfully")
                } else {
                    paymentRecharger.recharge(invoiceCharge)
                }
            }

    override fun getTask(): OneTimeTask<InvoiceCharge> {
        return chargeRechargeTask;
    }

}