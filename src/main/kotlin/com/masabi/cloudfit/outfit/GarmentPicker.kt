package com.masabi.cloudfit.outfit

import com.masabi.cloudfit.shared.Clothe
import com.masabi.cloudfit.shared.ClothingCategory
import com.masabi.cloudfit.shared.Event

/** Result of stage 2: which garments to wear, plus a short stylist note. */
data class Pick(
    val clotheIds: List<String>,
    val stylistNote: String,
)

/**
 * STAGE 2 of the AI pipeline — choose the garments that suit the event and combine well.
 *
 * Two implementations:
 *  - [RuleBasedPicker]   : deterministic, offline, used for tests and as a fallback.
 *  - [ClaudeGarmentPicker]: the real stylist, backed by an LLM.
 */
interface GarmentPicker {
    suspend fun pick(event: Event, wardrobe: List<Clothe>): Pick
}

/**
 * Deterministic fallback. Enforces the agreed rules:
 *   - a valid outfit is TOP + BOTTOM + SHOES, or a single DRESS + SHOES
 *   - OUTERWEAR and ACCESSORIES are optional add-ons
 */
class RuleBasedPicker : GarmentPicker {
    override suspend fun pick(event: Event, wardrobe: List<Clothe>): Pick {
        val byCategory = wardrobe.groupBy { it.category }
        fun first(category: ClothingCategory) = byCategory[category]?.firstOrNull()

        val chosen = buildList {
            val dress = first(ClothingCategory.DRESS)
            if (dress != null) {
                add(dress)
            } else {
                first(ClothingCategory.TOP)?.let { add(it) }
                first(ClothingCategory.BOTTOM)?.let { add(it) }
            }
            first(ClothingCategory.SHOES)?.let { add(it) }
            first(ClothingCategory.OUTERWEAR)?.let { add(it) }
            first(ClothingCategory.ACCESSORIES)?.let { add(it) }
        }
        val pieces = chosen.joinToString(", ") { it.category.name.lowercase() }
        return Pick(
            clotheIds = chosen.map { it.id },
            stylistNote = "Rule-based pick for \"${event.name}\": $pieces.",
        )
    }
}
