package app.cloudfit.shared.infrastructure.database

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

fun Instant.toUtc(): OffsetDateTime = atOffset(ZoneOffset.UTC)
