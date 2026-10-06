package app.cloudfit.wardrobe.application.usecase

import app.cloudfit.shared.application.error.ValidationException
import app.cloudfit.wardrobe.domain.ClotheDraft

internal object WardrobeValidation {
    private const val MAX_URL = 2048

    fun url(raw: String, field: String): String {
        val url = raw.trim()
        if (url.isEmpty() || url.length > MAX_URL) throw ValidationException("$field is required (max $MAX_URL chars)")
        return url
    }

    fun draft(draft: ClotheDraft): ClotheDraft = draft.copy(
        imageUrl = url(draft.imageUrl, "imageUrl"),
        name = text(draft.name, "name", 100),
        colour = text(draft.colour, "colour", 50),
        pattern = text(draft.pattern, "pattern", 50),
        description = text(draft.description, "description", 500),
    )

    private fun text(value: String?, field: String, max: Int): String? {
        val trimmed = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (trimmed.length > max) throw ValidationException("$field max $max characters")
        return trimmed
    }
}
