/*
    Implements endpoints related to invoices.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus

class InvoiceService(private val dal: AntaeusDal) {
    fun fetchAll(): List<Invoice> {
        return dal.fetchInvoices()
    }

    fun fetch(id: Int): Invoice {
        return dal.fetchInvoice(id) ?: throw InvoiceNotFoundException(id)
    }

    fun fetchPending(): List<Invoice> {
        return dal.fetchInvoicesByInvoiceStatus(InvoiceStatus.PENDING)
    }

    fun markInvoiceAsPaid(invoice: Invoice) {
        return dal.changeInvoiceStatus(invoice.id, InvoiceStatus.PAID)
    }

    fun markInvoiceAsUnpaid(invoice: Invoice) {
        return dal.changeInvoiceStatus(invoice.id, InvoiceStatus.UNPAID)
    }

}
