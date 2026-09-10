package com.masabi.cloudfit.outfit

import com.masabi.cloudfit.ai.Part
import com.masabi.cloudfit.ai.TextModel
import com.masabi.cloudfit.shared.ClothingCategory
import com.masabi.cloudfit.shared.Formality
import com.masabi.cloudfit.shared.Warmth
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

private class CannedTextModel(private val response: String) : TextModel {
    override suspend fun generate(model: String, systemPrompt: String, parts: List<Part>, asJson: Boolean) = response
}

class GeminiGarmentTaggerTest : StringSpec({

    "maps the model json into typed tags" {
        val tagger = GeminiGarmentTagger(
            CannedTextModel(
                """{"name":"Dark wash jeans","category":"bottom","color":"dark blue",
                   "pattern":"solid","formality":"smart casual","warmth":"mid",
                   "description":"dark wash straight-leg jeans"}""",
            ),
            "fake-model",
        )

        val tags = tagger.tag("PHOTO".toByteArray(), "image/jpeg")

        tags.name shouldBe "Dark wash jeans"
        tags.category shouldBe ClothingCategory.BOTTOM
        tags.colour shouldBe "dark blue"
        tags.pattern shouldBe "solid"
        tags.formality shouldBe Formality.SMART_CASUAL
        tags.warmth shouldBe Warmth.MEDIUM
    }

    "maps accessory to the ACCESSORIES category" {
        val tagger = GeminiGarmentTagger(
            CannedTextModel("""{"category":"accessory","color":"tan"}"""),
            "fake-model",
        )
        tagger.tag("x".toByteArray(), "image/png").category shouldBe ClothingCategory.ACCESSORIES
    }

    "throws on an unknown category" {
        val tagger = GeminiGarmentTagger(CannedTextModel("""{"category":"spaceship"}"""), "fake-model")
        shouldThrowAny { tagger.tag("x".toByteArray(), "image/png") }
    }
})
