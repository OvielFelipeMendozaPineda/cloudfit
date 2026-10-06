package app.cloudfit.styling.domain

import java.time.Instant
import java.util.UUID

enum class LookStatus {
    QUEUED,
    PICKING,
    RENDERING,
    READY,
    FAILED,
    ;

    val inProgress: Boolean get() = this == QUEUED || this == PICKING || this == RENDERING

    companion object {
        val IN_PROGRESS = entries.filter { it.inProgress }
    }
}

object LookFailureCodes {
    const val AI_UNAVAILABLE = "AI_UNAVAILABLE"
    const val WARDROBE_INCOMPLETE = "WARDROBE_INCOMPLETE"
    const val INTERRUPTED = "INTERRUPTED"
    const val INTERNAL_ERROR = "INTERNAL_ERROR"
}

data class Look(
    val id: UUID,
    val userId: UUID,
    val event: String,
    val status: LookStatus,
    val clotheIds: List<UUID>,
    val stylistNote: String?,
    val imageUrl: String?,
    val saved: Boolean,
    val failureCode: String?,
    val createdAt: Instant,
)
