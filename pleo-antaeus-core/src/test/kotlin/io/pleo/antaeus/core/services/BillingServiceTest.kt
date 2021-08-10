package io.pleo.antaeus.core.services

import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.task.TaskInstance
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.pleo.antaeus.core.infrastructure.DateTimeProvider
import io.pleo.antaeus.models.Currency.EUR
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal.TEN
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class BillingServiceTest {

    private val dateTimeProvider =
        DateTimeProvider(Clock.fixed(Instant.parse("2021-08-10T10:15:30.00Z"), ZoneId.systemDefault()))

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
        val (testInvoice, testInvoice2) = listOf(createInvoice(1), createInvoice(2))

        every { invoiceService.fetchPending() } returns listOf(testInvoice, testInvoice2)

        //when
        billingService.start()

        //then
        verify {
            invoiceService.fetchPending()
            schedulerClient.schedule(
                TaskInstance<InvoiceCharge>(PaymentChargeTask.PAYMENT_CHARGE_TASK_NAME, "1", InvoiceCharge(testInvoice)),
                dateTimeProvider.now()
            )
            schedulerClient.schedule(
                TaskInstance<InvoiceCharge>(PaymentChargeTask.PAYMENT_CHARGE_TASK_NAME, "2", InvoiceCharge(testInvoice2)),
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
