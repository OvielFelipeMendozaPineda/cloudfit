package app.cloudfit.shared.infrastructure.http

import app.cloudfit.shared.application.error.NotFoundException
import io.ktor.server.application.ApplicationCall
import java.util.UUID

fun ApplicationCall.uuidParameter(name: String): UUID =
    parameters[name]?.let { runCatching { UUID.fromString(it) }.getOrNull() } ?: throw NotFoundException()
