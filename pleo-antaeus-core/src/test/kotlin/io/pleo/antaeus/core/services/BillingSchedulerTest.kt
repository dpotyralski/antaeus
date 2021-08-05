package io.pleo.antaeus.core.services

import com.github.kagkarlsson.scheduler.task.ExecutionComplete
import com.github.kagkarlsson.scheduler.task.ExecutionContext
import com.github.kagkarlsson.scheduler.task.ExecutionOperations
import com.github.kagkarlsson.scheduler.task.TaskInstance
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import io.pleo.antaeus.core.exceptions.BillingSchedulerException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class BillingSchedulerTest {

    private val billingService = mockk<BillingService>(relaxUnitFun = true) {}
    private val billingScheduler = BillingScheduler("00 27 17 * * *", billingService)

    @Test
    fun `scheduler task should call proper initial method`() {
        //given
        val taskInstance = mockk<TaskInstance<Void>> {}
        val executionContext = mockk<ExecutionContext> {}

        //when
        billingScheduler.getTask().execute(taskInstance, executionContext)

        //then
        verify { billingService.start() }
    }

    @Test
    fun `should get proper exception in case of failure`() {
        //given
        val executionOperations = mockk<ExecutionOperations<Void>> {}
        val executionComplete =
            spyk<ExecutionComplete>(ExecutionComplete.failure(mockk(), mockk(), mockk(), IllegalArgumentException("test1"))) {}

        //when
        val exception = assertThrows<BillingSchedulerException> {
            billingScheduler.getTask()
                .failureHandler
                .onFailure(executionComplete, executionOperations)
        }

        assertEquals("Ups.. something went really wrong here, the main cause: ", exception.message)
    }

}