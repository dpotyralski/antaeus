package io.pleo.antaeus.core.services

import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.task.TaskInstance
import io.mockk.*
import io.pleo.antaeus.core.infrastructure.DateTimeProvider
import io.pleo.antaeus.models.Currency.EUR
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal.TEN
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class BillingServiceTest {

    private val dateTimeProvider =
        DateTimeProvider(Clock.fixed(Instant.parse("2007-12-03T10:15:30.00Z"), ZoneId.systemDefault()))

    private val invoiceService = mockk<InvoiceService> {}
    private val schedulerClient = mockk<SchedulerClient>(relaxed = true)

    private val billingService = BillingService(dateTimeProvider, schedulerClient, invoiceService)

    @BeforeEach
    fun init() {
        clearMocks(invoiceService)
    }

    @Test
    fun `should successfully schedule an invoice charge with 10 seconds difference between each`() {
        //given
        val testInvoice = createInvoice(1)
        val testInvoice2 = createInvoice(2)

        every { invoiceService.fetchPending() } returns listOf(testInvoice, testInvoice2)

        //when
        billingService.start()

        //then
        verify {
            invoiceService.fetchPending()
            schedulerClient.schedule(
                TaskInstance(PaymentChargeTask.PAYMENT_CHARGE_TASK_NAME, "1", testInvoice),
                dateTimeProvider.now()
            )
            schedulerClient.schedule(
                TaskInstance(PaymentChargeTask.PAYMENT_CHARGE_TASK_NAME, "2", testInvoice2),
                dateTimeProvider.now().plusSeconds(10)
            )
        }

        // no other calls were made
        confirmVerified(invoiceService)
    }

    @Test
    fun `should not schedule any invoice charge`() {
        //given
        every { invoiceService.fetchPending() } returns listOf()

        //when
        billingService.start()

        //then
        verify { invoiceService.fetchPending() }
        verify { schedulerClient wasNot Called }

        // no other calls were made
        confirmVerified(invoiceService)
    }

    private fun createInvoice(id: Int) = Invoice(id, 1, Money(TEN, EUR), InvoiceStatus.PENDING)

}
