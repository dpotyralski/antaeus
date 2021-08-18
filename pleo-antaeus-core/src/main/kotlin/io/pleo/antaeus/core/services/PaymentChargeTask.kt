package io.pleo.antaeus.core.services

import com.github.kagkarlsson.scheduler.task.ExecutionContext
import com.github.kagkarlsson.scheduler.task.FailureHandler
import com.github.kagkarlsson.scheduler.task.TaskInstance
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import io.pleo.antaeus.core.external.PaymentProvider
import mu.KotlinLogging
import java.time.Instant

private val logger = KotlinLogging.logger {}

class PaymentChargeTask(
    private val invoiceService: InvoiceService,
    private val paymentProvider: PaymentProvider,
    private val paymentRecharger: PaymentRecharger,
    private val failureHandler: FailureHandler<InvoiceCharge>
) : OnDemandSchedulerTask<InvoiceCharge> {

    companion object {
        const val PAYMENT_CHARGE_TASK_NAME = "payment-charge-task"
    }

    private val chargeRechargeTask: OneTimeTask<InvoiceCharge> =
        Tasks.oneTime(PAYMENT_CHARGE_TASK_NAME, InvoiceCharge::class.java)
            .onFailure(failureHandler)
            .execute { taskInstance: TaskInstance<InvoiceCharge>, _: ExecutionContext ->

                val invoiceCharge = taskInstance.data
                val invoice = invoiceCharge.invoice

                if (invoiceCharge.retryCounter >= 3) {
                    logger.info("Retry limit for invoice: $invoice has been reached, marking as unpaid.")
                    invoiceService.markInvoiceAsUnpaid(invoice)
                    return@execute
                }

                logger.info("Charging invoice: $invoice = at ${Instant.now()}")

                if (paymentProvider.charge(invoice)) {
                    invoiceService.markInvoiceAsPaid(invoice)
                    logger.info("Invoice with id: ${invoice.id} was charged successfully")
                } else {
                    paymentRecharger.recharge(invoiceCharge)
                }
            }

    override fun getTask(): OneTimeTask<InvoiceCharge> {
        return chargeRechargeTask
    }

}