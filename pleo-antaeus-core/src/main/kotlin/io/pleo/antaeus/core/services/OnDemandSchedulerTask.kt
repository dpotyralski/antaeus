package io.pleo.antaeus.core.services

import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask

interface OnDemandSchedulerTask<T> {

    fun getTask(): OneTimeTask<T>

}