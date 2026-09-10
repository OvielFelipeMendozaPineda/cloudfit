package com.masabi.cloudfit.ai

import com.masabi.cloudfit.clothes.Clothe
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class Pick(
    val clotheIds: List<String>,
    val stylistNote: String,
)

class ClothePicker(
    private val model: TextModel,
    private val modelName: String,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun pick(event: String, wardrobe: List<Clothe>): Pick {
        require(wardrobe.isNotEmpty()) { "wardrobe is empty" }

        val raw = model.generate(
            model = modelName,
            systemPrompt = SYSTEM_PROMPT,
            parts = listOf(Part(text = userPrompt(event, wardrobe))),
            asJson = true,
        )
        val parsed = json.decodeFromString<GeminiPick>(raw)

        val validIds = wardrobe.map { it.id }.toSet()
        val clotheIds = parsed.clotheIds.filter { it in validIds }.distinct()
        check(clotheIds.isNotEmpty()) { "model returned no valid clothe ids" }

        return Pick(
            clotheIds = clotheIds,
            stylistNote = parsed.stylistNote.ifBlank { "Picked for $event." },
        )
    }

    private fun userPrompt(event: String, wardrobe: List<Clothe>): String {
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
        return """
            Event: $event

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
- use ONLY clothe ids from the provided wardrobe — never invent ids

Respond with ONLY a JSON object in exactly this shape:
{"clotheIds": ["<id>", ...], "stylistNote": "<one or two short sentences>"}
"""
    }
}
