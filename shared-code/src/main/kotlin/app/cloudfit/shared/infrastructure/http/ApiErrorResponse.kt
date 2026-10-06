package app.cloudfit.shared.infrastructure.http

import kotlinx.serialization.Serializable

@Serializable
data class ApiErrorResponse(
    val error: String,
    val message: String,
)
