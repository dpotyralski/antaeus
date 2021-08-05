package io.pleo.antaeus.core.exceptions

class BillingSchedulerException : Exception {
    constructor() : super("Unexpected scheduler exception occurred")
    constructor(message: String, cause: Throwable) : super(message, cause)
}
