package app.cloudfit.shared.infrastructure.http

import app.cloudfit.shared.application.error.UnauthorizedException
import app.cloudfit.shared.domain.AuthenticatedUser
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.principal

fun ApplicationCall.requireAuthenticatedUser(): AuthenticatedUser =
    principal<AuthenticatedUser>() ?: throw UnauthorizedException()
