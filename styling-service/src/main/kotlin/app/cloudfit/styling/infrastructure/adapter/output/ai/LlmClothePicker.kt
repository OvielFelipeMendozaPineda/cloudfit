package app.cloudfit.styling.infrastructure.adapter.output.ai

import app.cloudfit.shared.application.ai.Part
import app.cloudfit.shared.application.ai.TextModel
import app.cloudfit.shared.domain.AppLocale
import app.cloudfit.styling.application.port.output.ClothePicker
import app.cloudfit.styling.domain.Pick
import app.cloudfit.styling.domain.WardrobeItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class LlmClothePicker(
    private val model: TextModel,
    private val modelName: String,
) : ClothePicker {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun pick(event: String, wardrobe: List<WardrobeItem>, locale: AppLocale): Pick {
        require(wardrobe.isNotEmpty()) { "wardrobe is empty" }

        val raw = model.generate(
            model = modelName,
            systemPrompt = SYSTEM_PROMPT,
            parts = listOf(Part(text = userPrompt(event, wardrobe, locale))),
            asJson = true,
        )
        val parsed = json.decodeFromString<GeminiPick>(raw)

        val byId = wardrobe.associateBy { it.id.toString() }
        val clotheIds = parsed.clotheIds.mapNotNull { byId[it.trim()]?.id }.distinct()
        check(clotheIds.isNotEmpty()) { "model returned no valid clothe ids" }

        return Pick(
            clotheIds = clotheIds,
            stylistNote = parsed.stylistNote.ifBlank { fallbackNote(event, locale) },
        )
    }

    private fun userPrompt(event: String, wardrobe: List<WardrobeItem>, locale: AppLocale): String {
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

            Write the stylistNote in ${languageOf(locale)}.
        """.trimIndent()
    }

    private fun languageOf(locale: AppLocale) = when (locale) {
        AppLocale.ES -> "Spanish (es)"
        AppLocale.EN -> "English (en)"
    }

    private fun fallbackNote(event: String, locale: AppLocale) = when (locale) {
        AppLocale.ES -> "Elegido para $event."
        AppLocale.EN -> "Picked for $event."
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
