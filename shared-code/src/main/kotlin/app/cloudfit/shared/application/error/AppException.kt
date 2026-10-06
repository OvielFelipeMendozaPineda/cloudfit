package app.cloudfit.shared.application.error

sealed class AppException(
    val code: String,
    override val message: String,
) : RuntimeException(message)

class ValidationException(message: String) : AppException("VALIDATION_ERROR", message)

class UnauthorizedException(
    message: String = "Unauthorized",
    code: String = "UNAUTHORIZED",
) : AppException(code, message)

class InsufficientCreditsException(message: String = "Not enough credits") : AppException("INSUFFICIENT_CREDITS", message)

class ForbiddenException(
    message: String = "Forbidden",
    code: String = "FORBIDDEN",
) : AppException(code, message)

class NotFoundException(message: String = "Not found") : AppException("NOT_FOUND", message)

class ConflictException(message: String) : AppException("CONFLICT", message)

class UnprocessableException(message: String, code: String) : AppException(code, message)

class RateLimitedException(message: String = "Too many requests") : AppException("RATE_LIMITED", message)

class ProviderNotConfiguredException(message: String) : AppException("PROVIDER_NOT_CONFIGURED", message)

class UpstreamException(
    message: String,
    code: String = "UPSTREAM_ERROR",
) : AppException(code, message)
