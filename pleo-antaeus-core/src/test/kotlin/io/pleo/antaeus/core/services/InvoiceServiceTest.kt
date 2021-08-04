package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class InvoiceServiceTest {
    private val dal = mockk<AntaeusDal>(relaxUnitFun = true) {
        every { fetchInvoice(404) } returns null
        every { fetchInvoicesByInvoiceStatus(InvoiceStatus.PENDING) } returns listOf(createInvoice())
    }

    private val invoiceService = InvoiceService(dal = dal)

    @Test
    fun `will throw if invoice is not found`() {
        assertThrows<InvoiceNotFoundException> {
            invoiceService.fetch(404)
        }
    }

    @Test
    fun `should call dal for only pending invoices`() {
        //when
        invoiceService.fetchPending()

        //then
        verify {
            dal.fetchInvoicesByInvoiceStatus(InvoiceStatus.PENDING)
        }
    }

    @Test
    fun `should mark invoice as paid`() {
        //given
        val testInvoice = createInvoice()

        //when
        invoiceService.markInvoiceAsPaid(testInvoice)

        //then
        verify {
            dal.changeInvoiceStatus(1, InvoiceStatus.PAID)
        }
    }

    @Test
    fun `should mark invoice as unpaid`() {
        //given
        val testInvoice = createInvoice()

        //when
        invoiceService.markInvoiceAsUnpaid(testInvoice)

        //then
        verify {
            dal.changeInvoiceStatus(1, InvoiceStatus.UNPAID)
        }
    }

    private fun createInvoice() = Invoice(1, 1, Money(BigDecimal("12"), Currency.EUR), status = InvoiceStatus.PENDING)

}
