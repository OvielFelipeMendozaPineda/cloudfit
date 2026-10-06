package app.cloudfit.shared.testing

import app.cloudfit.shared.application.ai.Part
import app.cloudfit.shared.application.ai.TextModel

class CannedTextModel(private val response: () -> String) : TextModel {
    constructor(response: String) : this({ response })

    val prompts = mutableListOf<List<Part>>()

    override suspend fun generate(model: String, systemPrompt: String, parts: List<Part>, asJson: Boolean): String {
        prompts += parts
        return response()
    }
}
