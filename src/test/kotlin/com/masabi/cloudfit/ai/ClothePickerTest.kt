package com.masabi.cloudfit.ai

import com.masabi.cloudfit.clothes.Clothe
import com.masabi.cloudfit.clothes.ClothingCategory
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

private class FakeTextModel(private val response: String) : TextModel {
    override suspend fun generate(model: String, systemPrompt: String, parts: List<Part>, asJson: Boolean) = response
}

class ClothePickerTest : StringSpec({

    fun clothe(id: String, category: ClothingCategory) =
        Clothe(id = id, category = category, imageUrl = "https://s3/$id.png")

    val wardrobe = listOf(
        clothe("t1", ClothingCategory.TOP),
        clothe("b1", ClothingCategory.BOTTOM),
        clothe("s1", ClothingCategory.SHOES),
    )

    "parses the model's json and keeps its order" {
        val picker = ClothePicker(
            FakeTextModel("""{"clotheIds":["t1","b1","s1"],"stylistNote":"Fresh."}"""),
            "fake-model",
        )
        val pick = picker.pick("brunch", wardrobe)
        pick.clotheIds shouldContainExactly listOf("t1", "b1", "s1")
        pick.stylistNote shouldBe "Fresh."
    }

    "drops ids the model invents that are not in the closet" {
        val picker = ClothePicker(
            FakeTextModel("""{"clotheIds":["t1","ghost","s1"],"stylistNote":"x"}"""),
            "fake-model",
        )
        picker.pick("brunch", wardrobe).clotheIds shouldContainExactly listOf("t1", "s1")
    }

    "throws when no valid ids remain" {
        val picker = ClothePicker(
            FakeTextModel("""{"clotheIds":["ghost"],"stylistNote":"x"}"""),
            "fake-model",
        )
        shouldThrowAny { picker.pick("brunch", wardrobe) }
    }
})
