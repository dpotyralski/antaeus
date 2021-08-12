package io.pleo.antaeus.core.services

import com.github.kagkarlsson.scheduler.task.FailureHandler
import com.github.kagkarlsson.scheduler.task.TaskInstance
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class PaymentChargeTaskTest {

    private val invoiceService = mockk<InvoiceService>(relaxUnitFun = true) {}
    private val paymentProvider = mockk<PaymentProvider> {}
    private val paymentRecharger = mockk<PaymentRecharger> {}
    private val failureHandler = mockk<FailureHandler<InvoiceCharge>> {}

    private val paymentChargeTask = PaymentChargeTask(invoiceService, paymentProvider, paymentRecharger, failureHandler)

    @Test
    fun `should mark pending invoice as paid when charged successfully`() {
        //given
        val invoice = createTestInvoice()
        every { paymentProvider.charge(invoice) } returns true

        //when
        paymentChargeTask.getTask().execute(TaskInstance("test", "1", InvoiceCharge(invoice)), mockk())

        //then
        verify {
            invoiceService.markInvoiceAsPaid(invoice)
        }

        // no other calls were made
        confirmVerified(invoiceService)
    }

    @Test
    fun `should mark pending invoice as unpaid when retry limit reached`() {
        //given
        val invoice = createTestInvoice()
        val invoiceCharge = InvoiceCharge(invoice)
        invoiceCharge.retryCounter = 3

        //when
        paymentChargeTask.getTask().execute(TaskInstance("test", "1", invoiceCharge), mockk())

        //then
        verify {
            invoiceService.markInvoiceAsUnpaid(invoice)
        }

        // no other calls were made
        confirmVerified(invoiceService)
    }

    private fun createTestInvoice() = Invoice(1, 1, Money(BigDecimal("12"), Currency.EUR), InvoiceStatus.PENDING)

}

