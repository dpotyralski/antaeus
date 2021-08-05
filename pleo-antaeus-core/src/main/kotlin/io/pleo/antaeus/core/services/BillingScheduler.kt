package io.pleo.antaeus.core.services

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import io.pleo.antaeus.core.exceptions.BillingSchedulerException

class BillingScheduler(cronPattern: String, billingService: BillingService) : OnStartupScheduledTask<Void> {

    private var cronTask: RecurringTask<Void> =
        Tasks.recurring("invoice-payment-cron", Schedules.cron(cronPattern))
            .onFailure { executionComplete, _ ->
                executionComplete.cause.map { t ->
                    throw BillingSchedulerException("Ups.. something went really wrong here, the main cause: ", t)
                }.orElseThrow { BillingSchedulerException() }
            }
            .execute { _, _ -> billingService.start() }

    override fun getTask(): RecurringTask<Void> {
        return cronTask
    }

}

