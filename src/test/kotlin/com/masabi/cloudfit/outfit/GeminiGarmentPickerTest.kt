package com.masabi.cloudfit.outfit

import com.masabi.cloudfit.ai.Part
import com.masabi.cloudfit.ai.TextModel
import com.masabi.cloudfit.shared.Clothe
import com.masabi.cloudfit.shared.ClothingCategory
import com.masabi.cloudfit.shared.Event
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/** Fake model: returns a canned response, so the picker is testable without hitting Gemini. */
private class FakeTextModel(private val response: String) : TextModel {
    override suspend fun generate(model: String, systemPrompt: String, parts: List<Part>, asJson: Boolean) = response
}

class GeminiGarmentPickerTest : StringSpec({

    fun clothe(id: String, category: ClothingCategory) =
        Clothe(id = id, category = category, imageUrl = "https://s3/$id.png")

    val wardrobe = listOf(
        clothe("t1", ClothingCategory.TOP),
        clothe("b1", ClothingCategory.BOTTOM),
        clothe("s1", ClothingCategory.SHOES),
    )
    val event = Event(id = "e1", name = "brunch")

    "parses the model's json and keeps its order" {
        val picker = GeminiGarmentPicker(
            FakeTextModel("""{"clotheIds":["t1","b1","s1"],"stylistNote":"Fresh."}"""),
            "fake-model",
        )
        val pick = picker.pick(event, wardrobe)
        pick.clotheIds shouldContainExactly listOf("t1", "b1", "s1")
        pick.stylistNote shouldBe "Fresh."
    }

    "drops ids the model invents that are not in the closet" {
        val picker = GeminiGarmentPicker(
            FakeTextModel("""{"clotheIds":["t1","ghost","s1"],"stylistNote":"x"}"""),
            "fake-model",
        )
        picker.pick(event, wardrobe).clotheIds shouldContainExactly listOf("t1", "s1")
    }

    "throws when no valid ids remain" {
        val picker = GeminiGarmentPicker(
            FakeTextModel("""{"clotheIds":["ghost"],"stylistNote":"x"}"""),
            "fake-model",
        )
        shouldThrowAny { picker.pick(event, wardrobe) }
    }
})
