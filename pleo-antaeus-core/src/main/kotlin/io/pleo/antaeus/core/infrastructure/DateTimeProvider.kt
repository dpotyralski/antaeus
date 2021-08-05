package io.pleo.antaeus.core.infrastructure

import java.time.Clock
import java.time.Instant

class DateTimeProvider(private val clock: Clock) {

    fun now(): Instant {
        return Instant.now(clock)
    }

}
