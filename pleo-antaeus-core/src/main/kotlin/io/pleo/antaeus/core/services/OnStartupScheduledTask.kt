package io.pleo.antaeus.core.services

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask

interface OnStartupScheduledTask<T> {

    fun getTask(): RecurringTask<T>

}