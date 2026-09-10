package com.masabi.cloudfit.ai

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

interface TextModel {
    suspend fun generate(
        model: String,
        systemPrompt: String,
        parts: List<Part>,
        asJson: Boolean = false,
    ): String
}
