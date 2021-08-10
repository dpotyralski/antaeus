package io.pleo.antaeus.core.services

import io.pleo.antaeus.models.Invoice
import java.io.Serializable

data class InvoiceCharge(val invoice: Invoice) : Serializable {
    var retryCounter = 0
    fun increaseRetryCounter() {
        this.retryCounter = retryCounter + 1
    }
}

