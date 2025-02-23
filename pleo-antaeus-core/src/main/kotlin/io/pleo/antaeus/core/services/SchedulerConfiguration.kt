package io.pleo.antaeus.core.services

import com.github.kagkarlsson.scheduler.Scheduler
import java.time.Duration
import javax.sql.DataSource

class SchedulerConfiguration(
    dataSource: DataSource,
    onStartupScheduledTasks: List<OnStartupScheduledTask<*>>,
    onDemandSchedulerTasks: List<OnDemandSchedulerTask<*>>
) {

    val scheduler: Scheduler = Scheduler
        .create(dataSource, onDemandSchedulerTasks.map(OnDemandSchedulerTask<*>::getTask))
        .startTasks(onStartupScheduledTasks.map(OnStartupScheduledTask<*>::getTask))
        .pollingInterval(Duration.ofSeconds(1))
        .build()

    init {
        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                scheduler.stop()
            }
        })
        scheduler.start()
    }

}
