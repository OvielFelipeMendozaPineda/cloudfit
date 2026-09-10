package com.masabi.cloudfit.outfit

import com.masabi.cloudfit.ai.Part
import com.masabi.cloudfit.ai.TextModel
import com.masabi.cloudfit.shared.Clothe
import com.masabi.cloudfit.shared.Event
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * STAGE 2, for real — asks a [TextModel] (Gemini) to assemble an outfit from the user's wardrobe.
 *
 * The model receives the event + the full wardrobe inventory (as structured text) and returns the
 * chosen garment ids plus a one/two-sentence stylist note. We then **enforce valid ids** (drop any
 * id it invents that isn't in the closet) so the frontend never has to reconcile.
 *
 * Resilience is *not* this class's job — wrap it in [FallbackGarmentPicker] to fall back to rules
 * when the model is unavailable. On an empty/invalid result this throws so the decorator can react.
 */
class GeminiGarmentPicker(
    private val model: TextModel,
    private val modelName: String,
) : GarmentPicker {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun pick(event: Event, wardrobe: List<Clothe>): Pick {
        require(wardrobe.isNotEmpty()) { "wardrobe is empty" }

        val raw = model.generate(
            model = modelName,
            systemPrompt = SYSTEM_PROMPT,
            parts = listOf(Part(text = userPrompt(event, wardrobe))),
            asJson = true,
        )
        val parsed = json.decodeFromString<GeminiPick>(raw)

        // Valid-ID enforcement: keep only ids that actually exist in this closet, in order.
        val validIds = wardrobe.map { it.id }.toSet()
        val clotheIds = parsed.clotheIds.filter { it in validIds }.distinct()
        check(clotheIds.isNotEmpty()) { "model returned no valid garment ids" }

        return Pick(
            clotheIds = clotheIds,
            stylistNote = parsed.stylistNote.ifBlank { "Picked for ${event.name}." },
        )
    }

    private fun userPrompt(event: Event, wardrobe: List<Clothe>): String {
        val inventory = wardrobe.joinToString("\n") { c ->
            val tags = listOfNotNull(
                c.name,
                c.colour,
                c.pattern,
                c.formality?.name?.lowercase(),
                c.warmth?.name?.lowercase(),
                c.description,
            ).joinToString(", ")
            "- id=${c.id} | ${c.category.name.lowercase()} | $tags"
        }
        val eventLine = listOfNotNull(event.name, event.description).joinToString(" — ")
        return """
            Event: $eventLine

            Wardrobe:
            $inventory
        """.trimIndent()
    }

    @Serializable
    private data class GeminiPick(
        val clotheIds: List<String> = emptyList(),
        val stylistNote: String = "",
    )

    private companion object {
        const val SYSTEM_PROMPT = """
You are a fashion stylist. Given an event and a wardrobe inventory, pick ONE outfit.

Rules for a valid outfit:
- (at least one TOP and at least one BOTTOM) OR a single DRESS
- always include SHOES
- OUTERWEAR and ACCESSORIES are optional; add them only if they improve the look
- pick pieces that suit the event's formality and that combine well in colour and style
- use ONLY garment ids from the provided wardrobe — never invent ids

Respond with ONLY a JSON object in exactly this shape:
{"clotheIds": ["<id>", ...], "stylistNote": "<one or two short sentences>"}
"""
    }
}
