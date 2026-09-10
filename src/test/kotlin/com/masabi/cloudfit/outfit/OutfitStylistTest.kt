package com.masabi.cloudfit.outfit

import com.masabi.cloudfit.shared.Clothe
import com.masabi.cloudfit.shared.ClothingCategory
import com.masabi.cloudfit.shared.Event
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe

class OutfitStylistTest : StringSpec({

    val stylist = OutfitStylist()

    fun clothe(id: String, category: ClothingCategory) =
        Clothe(id = id, category = category, imageUrl = "https://s3/$id.png")

    "composes top + bottom + shoes when there is no dress" {
        val outfit = stylist.compose(
            event = Event(id = "e1", name = "brunch"),
            wardrobe = listOf(
                clothe("t1", ClothingCategory.TOP),
                clothe("b1", ClothingCategory.BOTTOM),
                clothe("s1", ClothingCategory.SHOES),
            ),
        )
        outfit.clotheIds shouldContainAll listOf("t1", "b1", "s1")
    }

    "prefers a dress over separate top and bottom" {
        val outfit = stylist.compose(
            event = Event(id = "e1", name = "wedding"),
            wardrobe = listOf(
                clothe("t1", ClothingCategory.TOP),
                clothe("b1", ClothingCategory.BOTTOM),
                clothe("d1", ClothingCategory.DRESS),
                clothe("s1", ClothingCategory.SHOES),
            ),
        )
        outfit.clotheIds shouldContainAll listOf("d1", "s1")
        outfit.clotheIds.contains("t1") shouldBe false
    }
})
