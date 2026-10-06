package app.cloudfit.shared.application.ai

import java.util.UUID

data class AiUsage(
    val userId: UUID,
    val operation: String,
    val model: String,
    val inputTokens: Int,
    val outputTokens: Int,
)

fun interface AiUsageRecorder {
    suspend fun record(usage: AiUsage)
}
