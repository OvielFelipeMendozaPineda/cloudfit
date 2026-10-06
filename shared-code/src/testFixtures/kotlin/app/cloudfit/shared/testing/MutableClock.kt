package app.cloudfit.shared.testing

import app.cloudfit.shared.application.port.ClockProvider
import java.time.Duration
import java.time.Instant

class MutableClock(var current: Instant = Instant.parse("2026-10-06T12:00:00Z")) : ClockProvider {
    override fun now(): Instant = current

    fun advance(duration: Duration) {
        current = current.plus(duration)
    }
}
