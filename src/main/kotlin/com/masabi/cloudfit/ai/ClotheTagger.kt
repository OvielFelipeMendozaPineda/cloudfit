package com.masabi.cloudfit.ai

import com.masabi.cloudfit.clothes.ClothingCategory
import com.masabi.cloudfit.clothes.Formality
import com.masabi.cloudfit.clothes.Warmth
import java.util.Base64
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ClotheTags(
    val name: String?,
    val category: ClothingCategory,
    val colour: String?,
    val pattern: String?,
    val formality: Formality?,
    val warmth: Warmth?,
    val description: String?,
)

class ClotheTagger(
    private val model: TextModel,
    private val modelName: String,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun tag(imageBytes: ByteArray, mimeType: String): ClotheTags {
        val part = Part(inlineData = InlineData(mimeType, Base64.getEncoder().encodeToString(imageBytes)))
        val raw = model.generate(modelName, SYSTEM_PROMPT, listOf(part), asJson = true)
        val wire = json.decodeFromString<WireTags>(raw)

        return ClotheTags(
            name = wire.name,
            category = parseCategory(wire.category),
            colour = wire.color,
            pattern = wire.pattern,
            formality = parseFormality(wire.formality),
            warmth = parseWarmth(wire.warmth),
            description = wire.description,
        )
    }

    private fun parseCategory(value: String?): ClothingCategory = when (value?.trim()?.lowercase()) {
        "top" -> ClothingCategory.TOP
        "bottom" -> ClothingCategory.BOTTOM
        "dress" -> ClothingCategory.DRESS
        "outerwear" -> ClothingCategory.OUTERWEAR
        "shoes" -> ClothingCategory.SHOES
        "accessory", "accessories" -> ClothingCategory.ACCESSORIES
        else -> error("unknown category: $value")
    }

    private fun parseFormality(value: String?): Formality? = when (value?.trim()?.lowercase()) {
        "casual" -> Formality.CASUAL
        "smart casual", "smart_casual" -> Formality.SMART_CASUAL
        "formal" -> Formality.FORMAL
        else -> null
    }

    private fun parseWarmth(value: String?): Warmth? = when (value?.trim()?.lowercase()) {
        "light" -> Warmth.LIGHT
        "mid", "medium" -> Warmth.MEDIUM
        "warm" -> Warmth.WARM
        else -> null
    }

    @Serializable
    private data class WireTags(
        val name: String? = null,
        val category: String? = null,
        @SerialName("color") val color: String? = null,
        val pattern: String? = null,
        val formality: String? = null,
        val warmth: String? = null,
        val description: String? = null,
    )

    private companion object {
        const val SYSTEM_PROMPT = """
You are a clothe tagger for a virtual wardrobe app.
You receive one photo of a single clothing item, shoe, or accessory.

Return ONLY a JSON object. No markdown, no commentary.

{
  "name":        string,   // 2-4 words, how a person would say it
  "category":    "top" | "bottom" | "dress" | "outerwear" | "shoes" | "accessory",
  "color":       string,   // simple name: "black", "dark blue", "cream"
  "pattern":     "solid" | "striped" | "print" | "textured",
  "formality":   "casual" | "smart casual" | "formal",
  "warmth":      "light" | "mid" | "warm",
  "description": string    // one short phrase: cut, length, wash, material
}

Category rules:
- outerwear = worn OVER another top: jackets, blazers, coats, cardigans
- a long-sleeve button-down shirt is a top, not outerwear
- dress = one piece covering torso and legs; it replaces top + bottom
- accessory = bags, sunglasses, belts, hats, scarves, jewellery

If the photo shows several items, tag the most prominent one.
"""
    }
}
