package app.cloudfit.shared.infrastructure.time

import app.cloudfit.shared.application.port.ClockProvider
import java.time.Instant

class SystemClockProvider : ClockProvider {
    override fun now(): Instant = Instant.now()
}
