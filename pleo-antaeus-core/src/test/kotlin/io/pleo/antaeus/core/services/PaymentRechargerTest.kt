package io.pleo.antaeus.core.services

import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.task.TaskInstance
import io.mockk.mockk
import io.mockk.verify
import io.pleo.antaeus.core.infrastructure.DateTimeProvider
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class PaymentRechargerTest {

    private val dateTimeProvider =
        DateTimeProvider(Clock.fixed(Instant.parse("2021-08-10T10:15:30.00Z"), ZoneId.systemDefault()))
    private val schedulerClient = mockk<SchedulerClient>(relaxed = true)

    private val paymentRecharger = PaymentRecharger(dateTimeProvider, schedulerClient)

    @Test
    fun `should reschedule next payment and increase the retry counter`() {
        //given
        val invoiceCharge = InvoiceCharge(Invoice(1, 1, Money(BigDecimal("12"), Currency.EUR), InvoiceStatus.PENDING))

        //when
        paymentRecharger.recharge(invoiceCharge)

        //then
        verify {
            schedulerClient.schedule(
                TaskInstance<InvoiceCharge>(
                    PaymentChargeTask.PAYMENT_CHARGE_TASK_NAME,
                    "invoice-1-reschedule-attempt-1",
                    invoiceCharge
                ), Instant.parse("2021-08-10T10:15:40.00Z")
            )
        }

        //and
        assert(invoiceCharge.retryCounter == 1)
    }
}