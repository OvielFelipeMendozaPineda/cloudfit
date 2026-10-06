package app.cloudfit.shared.application.port

import java.time.Instant

fun interface ClockProvider {
    fun now(): Instant
}
