package com.masabi.cloudfit.outfit

import com.masabi.cloudfit.shared.Clothe
import com.masabi.cloudfit.shared.ClothingCategory
import com.masabi.cloudfit.shared.Event
import com.masabi.cloudfit.shared.Outfit
import java.util.UUID

/**
 * The AI brain of cloud-fit (Felipe's scope). Two conceptual steps:
 *
 *  1. STYLIST  — reason over the wardrobe + event and pick garments that suit and combine.
 *                Intended to be backed by an LLM that can *see* the garment images (e.g. Claude).
 *  2. RENDER   — take the picked outfit + avatar photo and generate the "model wearing it" image
 *                (an image model such as Gemini), then upload it to S3.
 *
 * The implementation below is a deterministic placeholder that already enforces the agreed
 * composition rules, so the flow is demoable end-to-end. Swap [pickGarments] and [renderImage]
 * for the real provider calls.
 *
 * This is called directly (in-process) by the wardrobe routes — no HTTP hop.
 */
class OutfitStylist {

    fun compose(event: Event, wardrobe: List<Clothe>, avatarImageUrl: String? = null): Outfit {
        val picked = pickGarments(wardrobe)
        val imageUrl = renderImage(picked, avatarImageUrl)

        return Outfit(
            id = UUID.randomUUID().toString(),
            clotheIds = picked.map { it.id },
            generatedImageUrl = imageUrl,
            rationale = buildRationale(picked, event),
        )
    }

    /**
     * STEP 1 — placeholder selection. Rules agreed in the group:
     *   - a valid outfit is TOP + BOTTOM + SHOES, or a single DRESS + SHOES
     *   - OUTERWEAR and ACCESSORIES are optional add-ons
     *
     * TODO(felipe): replace with an LLM call that receives the garment image URLs + the event and
     * returns the chosen garment ids with reasoning.
     */
    private fun pickGarments(wardrobe: List<Clothe>): List<Clothe> {
        val byCategory = wardrobe.groupBy { it.category }
        fun first(category: ClothingCategory) = byCategory[category]?.firstOrNull()

        return buildList {
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
    }

    /**
     * STEP 2 — placeholder render.
     *
     * TODO(felipe): call the image model (Gemini/etc.) with the avatar photo + the chosen garment
     * images, then upload the result to S3 and return its URL.
     */
    private fun renderImage(garments: List<Clothe>, avatarImageUrl: String?): String? {
        return null
    }

    private fun buildRationale(garments: List<Clothe>, event: Event): String {
        val pieces = garments.joinToString(", ") { it.category.name.lowercase() }
        return "Placeholder pick for \"${event.name}\": $pieces."
    }
}
