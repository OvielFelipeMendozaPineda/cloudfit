package app.cloudfit.shared.application.ai

interface TextModel {
    suspend fun generate(
        model: String,
        systemPrompt: String,
        parts: List<Part>,
        asJson: Boolean = false,
    ): String
}
