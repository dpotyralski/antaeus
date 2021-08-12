package io.pleo.antaeus.core.services

import com.github.kagkarlsson.scheduler.task.ExecutionComplete
import com.github.kagkarlsson.scheduler.task.ExecutionOperations
import com.github.kagkarlsson.scheduler.task.FailureHandler
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.infrastructure.DateTimeProvider
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class PaymentChargeFailureHandler(
    private val dateTimeProvider: DateTimeProvider
) : FailureHandler<InvoiceCharge> {
    override fun onFailure(
        executionComplete: ExecutionComplete,
        executionOperations: ExecutionOperations<InvoiceCharge>
    ) {
        val consecutiveFailures = executionComplete.execution.consecutiveFailures

        when (executionComplete.cause.get()) {
            is NetworkException -> {
                if (consecutiveFailures >= 3) {
                    logger.error("Payment charge execution failed due to network error - no more retries.")
                    executionOperations.stop()
                } else {
                    logger.warn("Network error, rescheduling payment, attempt: ${consecutiveFailures + 1} of 3.")
                    executionOperations.reschedule(executionComplete, dateTimeProvider.now().plusSeconds(5))
                }
            }
            is CustomerNotFoundException, is CurrencyMismatchException -> {
                logger.error("Error occurred, stopping task execution, main cause: ", executionComplete.cause.get())
                executionOperations.stop()
            }
        }
    }
}
