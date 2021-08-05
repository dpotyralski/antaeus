package io.pleo.antaeus.core.infrastructure

import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

internal class DateTimeProviderTest {

    private val dateTimeProvider =
        DateTimeProvider(clock = Clock.fixed(Instant.parse("2007-12-03T10:15:30.00Z"), ZoneId.systemDefault()))

    @Test
    fun `should get current date time`() {
        //expect
        assert(dateTimeProvider.now() == Instant.parse("2007-12-03T10:15:30.00Z"))
    }

}