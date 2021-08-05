package io.pleo.antaeus.models

import java.io.Serializable

data class Invoice(
    val id: Int,
    val customerId: Int,
    val amount: Money,
    val status: InvoiceStatus
) : Serializable
