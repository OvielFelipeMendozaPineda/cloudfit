package app.cloudfit.wardrobe.infrastructure.adapter.output.ai

import app.cloudfit.shared.domain.ClothingCategory
import app.cloudfit.shared.domain.Formality
import app.cloudfit.shared.domain.Warmth
import app.cloudfit.shared.testing.CannedTextModel
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class LlmClotheTaggerTest : StringSpec({

    "maps the model json into typed tags" {
        val tagger = LlmClotheTagger(
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
        val tagger = LlmClotheTagger(CannedTextModel("""{"category":"accessory","color":"tan"}"""), "fake-model")
        tagger.tag("x".toByteArray(), "image/png").category shouldBe ClothingCategory.ACCESSORIES
    }

    "throws on an unknown category" {
        val tagger = LlmClotheTagger(CannedTextModel("""{"category":"spaceship"}"""), "fake-model")
        shouldThrowAny { tagger.tag("x".toByteArray(), "image/png") }
    }
})
