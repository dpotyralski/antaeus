package io.pleo.antaeus.core.services

import com.github.kagkarlsson.scheduler.task.Execution
import com.github.kagkarlsson.scheduler.task.ExecutionComplete
import com.github.kagkarlsson.scheduler.task.ExecutionOperations
import com.github.kagkarlsson.scheduler.task.TaskInstance
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.infrastructure.DateTimeProvider
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.stream.Stream


internal class PaymentChargeFailureHandlerTest {

    private val dateTimeProvider =
        DateTimeProvider(Clock.fixed(Instant.parse("2021-08-10T10:15:30.00Z"), ZoneId.systemDefault()))
    private val testInvoice = Invoice(1, 1, Money(BigDecimal("12"), Currency.EUR), InvoiceStatus.PENDING)

    private val executionOperations = mockk<ExecutionOperations<InvoiceCharge>>(relaxUnitFun = true)

    private val paymentChargeFailureHandler = PaymentChargeFailureHandler(dateTimeProvider)

    @ParameterizedTest(name = "should reschedule invoice charge within 5 seconds if attempt limit was not reached (attempt: {0}) in case of NetworkException")
    @ValueSource(ints = [0, 1, 2])
    fun `should reschedule next invoice charge within 5 seconds in case of network issues`(failures: Int) {
        //given
        val execution = createFakeExecution(failures = failures)
        val executionComplete = ExecutionComplete.failure(execution, mockk(), mockk(), NetworkException())

        //when
        paymentChargeFailureHandler.onFailure(executionComplete, executionOperations)

        //then
        verify {
            executionOperations.reschedule(executionComplete, Instant.parse("2021-08-10T10:15:35.00Z"))
        }
    }

    @Test
    fun `should not reschedule and mark invoice as unpaid when max attempt reached in case of network issues`() {
        //given
        val execution = createFakeExecution(failures = 3)
        val executionComplete = ExecutionComplete.failure(execution, mockk(), mockk(), NetworkException())

        //when
        paymentChargeFailureHandler.onFailure(executionComplete, executionOperations)

        //then
        verify {
            executionOperations.stop()
        }
    }

    @ParameterizedTest(name = "should stop the execution immediately in case of exception: {0}")
    @MethodSource("providePossibleExceptions")
    fun `should stop the execution in case of issues`(exception: Exception) {
        //given
        val execution = createFakeExecution()
        val executionComplete = ExecutionComplete.failure(execution, mockk(), mockk(), exception)

        //when
        paymentChargeFailureHandler.onFailure(executionComplete, executionOperations)

        //then
        verify {
            executionOperations.stop()
        }
    }

    companion object {
        @JvmStatic
        private fun providePossibleExceptions(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(CustomerNotFoundException(1)),
                Arguments.of(CurrencyMismatchException(1, 1))
            )
        }
    }

    private fun createFakeExecution(failures: Int = 0) = spyk(
        Execution(
            mockk(),
            TaskInstance("", "", InvoiceCharge(testInvoice)),
            true,
            "",
            mockk(),
            mockk(),
            failures,
            mockk(),
            1
        )
    )

}