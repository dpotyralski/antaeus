package io.pleo.antaeus.core.services

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class BillingScheduler(cronPattern: String, billingService: BillingService) : OnStartupScheduledTask<Void> {

    companion object {
        val logger: Logger = LoggerFactory.getLogger(BillingScheduler::class.java)
    }

    private var cronTask: RecurringTask<Void> =
        Tasks.recurring("invoice-payment-cron", Schedules.cron(cronPattern))
            .execute { _, _ -> billingService.start() }

    override fun getTask(): RecurringTask<Void> {
        return cronTask
    }

}

