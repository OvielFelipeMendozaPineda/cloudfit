package app.cloudfit.styling.infrastructure.adapter.input.http.dto

import app.cloudfit.styling.domain.Look
import kotlinx.serialization.Serializable

@Serializable
data class CreateLookRequestDto(val event: String = "")

@Serializable
data class LookDto(
    val id: String,
    val event: String,
    val status: String,
    val clotheIds: List<String>,
    val stylistNote: String?,
    val imageUrl: String?,
    val saved: Boolean,
    val failureCode: String?,
    val createdAt: String,
) {
    companion object {
        fun from(look: Look) = LookDto(
            id = look.id.toString(),
            event = look.event,
            status = look.status.name,
            clotheIds = look.clotheIds.map { it.toString() },
            stylistNote = look.stylistNote,
            imageUrl = look.imageUrl,
            saved = look.saved,
            failureCode = look.failureCode,
            createdAt = look.createdAt.toString(),
        )
    }
}
