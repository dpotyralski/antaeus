package io.pleo.antaeus.core.services

import com.github.kagkarlsson.scheduler.task.ExecutionContext
import com.github.kagkarlsson.scheduler.task.TaskInstance
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

internal class BillingSchedulerTest {

    private val billingService = mockk<BillingService>(relaxUnitFun = true) {}

    @Test
    fun `scheduler task should call proper initial method`() {
        //given
        val taskInstance = mockk<TaskInstance<Void>> {}
        val executionContext = mockk<ExecutionContext> {}

        val billingScheduler = BillingScheduler("00 27 17 * * *", billingService)

        //when
        billingScheduler.getTask().execute(taskInstance, executionContext)

        //then
        verify { billingService.start() }
    }

}