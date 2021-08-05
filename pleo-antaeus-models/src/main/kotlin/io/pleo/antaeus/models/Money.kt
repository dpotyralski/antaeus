package io.pleo.antaeus.models

import java.io.Serializable
import java.math.BigDecimal

data class Money(
    val value: BigDecimal,
    val currency: Currency
) : Serializable
