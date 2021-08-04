package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.PaymentProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant

class BillingService(
    private val paymentProvider: PaymentProvider
) {

    companion object {
        val logger: Logger = LoggerFactory.getLogger(BillingService::class.java)
    }

    fun start() {
        logger.info("Invoice charging process started at ${Instant.now()}")
        //todo
    }

}
