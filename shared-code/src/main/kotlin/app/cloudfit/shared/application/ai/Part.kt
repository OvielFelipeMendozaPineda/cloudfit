package app.cloudfit.shared.application.ai

import kotlinx.serialization.Serializable

@Serializable
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null,
)

@Serializable
data class InlineData(
    val mimeType: String,
    val data: String,
)
