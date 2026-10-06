package app.cloudfit.shared.infrastructure.http

import app.cloudfit.shared.application.error.AppException
import app.cloudfit.shared.application.error.ConflictException
import app.cloudfit.shared.application.error.ErrorCodes
import app.cloudfit.shared.application.error.ForbiddenException
import app.cloudfit.shared.application.error.InsufficientCreditsException
import app.cloudfit.shared.application.error.NotFoundException
import app.cloudfit.shared.application.error.ProviderNotConfiguredException
import app.cloudfit.shared.application.error.RateLimitedException
import app.cloudfit.shared.application.error.UnauthorizedException
import app.cloudfit.shared.application.error.UnprocessableException
import app.cloudfit.shared.application.error.UpstreamException
import app.cloudfit.shared.application.error.ValidationException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond

fun AppException.httpStatus(): HttpStatusCode = when (this) {
    is ValidationException -> HttpStatusCode.BadRequest
    is UnauthorizedException -> HttpStatusCode.Unauthorized
    is InsufficientCreditsException -> HttpStatusCode.PaymentRequired
    is ForbiddenException -> HttpStatusCode.Forbidden
    is NotFoundException -> HttpStatusCode.NotFound
    is ConflictException -> HttpStatusCode.Conflict
    is UnprocessableException -> HttpStatusCode.UnprocessableEntity
    is RateLimitedException -> HttpStatusCode.TooManyRequests
    is ProviderNotConfiguredException -> HttpStatusCode.NotImplemented
    is UpstreamException -> HttpStatusCode.BadGateway
}

fun Application.configureApiStatusPages() {
    install(StatusPages) {
        exception<AppException> { call, cause ->
            call.respond(cause.httpStatus(), ApiErrorResponse(cause.code, cause.message))
        }
        exception<BadRequestException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ApiErrorResponse("VALIDATION_ERROR", cause.message ?: "Bad request"))
        }
        exception<Throwable> { call, cause ->
            call.application.environment.log.error("Unhandled error", cause)
            call.respond(HttpStatusCode.InternalServerError, ApiErrorResponse(ErrorCodes.INTERNAL_ERROR, "Unexpected error"))
        }
        status(HttpStatusCode.TooManyRequests) { call, status ->
            call.respond(status, ApiErrorResponse("RATE_LIMITED", "Too many requests"))
        }
        status(HttpStatusCode.NotFound) { call, status ->
            call.respond(status, ApiErrorResponse("NOT_FOUND", "Not found"))
        }
    }
}
