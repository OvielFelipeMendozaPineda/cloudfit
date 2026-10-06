package app.cloudfit.styling.domain

import app.cloudfit.shared.domain.ClothingCategory
import app.cloudfit.shared.domain.Formality
import app.cloudfit.shared.domain.Warmth
import java.util.UUID

data class WardrobeItem(
    val id: UUID,
    val category: ClothingCategory,
    val imageUrl: String,
    val name: String? = null,
    val colour: String? = null,
    val pattern: String? = null,
    val formality: Formality? = null,
    val warmth: Warmth? = null,
    val description: String? = null,
)

data class WardrobeSnapshot(
    val items: List<WardrobeItem>,
    val avatarUrl: String?,
) {
    val isComplete: Boolean
        get() {
            val categories = items.map { it.category }.toSet()
            val hasBody = (ClothingCategory.TOP in categories && ClothingCategory.BOTTOM in categories) ||
                ClothingCategory.DRESS in categories
            return hasBody && ClothingCategory.SHOES in categories
        }
}

data class Pick(
    val clotheIds: List<UUID>,
    val stylistNote: String,
)
