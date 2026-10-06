package app.cloudfit.styling.infrastructure.adapter.output.ai

import app.cloudfit.shared.domain.AppLocale
import app.cloudfit.shared.domain.ClothingCategory
import app.cloudfit.shared.testing.CannedTextModel
import app.cloudfit.styling.domain.WardrobeItem
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.util.UUID

class LlmClothePickerTest : StringSpec({

    fun clothe(category: ClothingCategory) = WardrobeItem(id = UUID.randomUUID(), category = category, imageUrl = "https://s3/x.png")

    val t1 = clothe(ClothingCategory.TOP)
    val b1 = clothe(ClothingCategory.BOTTOM)
    val s1 = clothe(ClothingCategory.SHOES)
    val wardrobe = listOf(t1, b1, s1)

    "parses the model's json and keeps its order" {
        val picker = LlmClothePicker(
            CannedTextModel("""{"clotheIds":["${t1.id}","${b1.id}","${s1.id}"],"stylistNote":"Fresh."}"""),
            "fake-model",
        )
        val pick = picker.pick("brunch", wardrobe, AppLocale.EN)
        pick.clotheIds shouldContainExactly listOf(t1.id, b1.id, s1.id)
        pick.stylistNote shouldBe "Fresh."
    }

    "drops ids the model invents that are not in the closet" {
        val picker = LlmClothePicker(
            CannedTextModel("""{"clotheIds":["${t1.id}","ghost","${s1.id}"],"stylistNote":"x"}"""),
            "fake-model",
        )
        picker.pick("brunch", wardrobe, AppLocale.EN).clotheIds shouldContainExactly listOf(t1.id, s1.id)
    }

    "throws when no valid ids remain" {
        val picker = LlmClothePicker(CannedTextModel("""{"clotheIds":["ghost"],"stylistNote":"x"}"""), "fake-model")
        shouldThrowAny { picker.pick("brunch", wardrobe, AppLocale.EN) }
    }

    "asks for the stylist note in the user's locale and falls back in that language" {
        val model = CannedTextModel("""{"clotheIds":["${t1.id}"],"stylistNote":""}""")
        val pick = LlmClothePicker(model, "fake-model").pick("boda", wardrobe, AppLocale.ES)

        model.prompts.single().single().text!! shouldContain "Spanish (es)"
        pick.stylistNote shouldBe "Elegido para boda."
    }
})
