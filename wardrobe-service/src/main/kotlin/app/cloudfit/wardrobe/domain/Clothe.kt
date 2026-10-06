package app.cloudfit.wardrobe.domain

import app.cloudfit.shared.domain.ClothingCategory
import app.cloudfit.shared.domain.Formality
import app.cloudfit.shared.domain.Warmth
import java.util.UUID

data class ClotheDraft(
    val category: ClothingCategory,
    val imageUrl: String,
    val name: String? = null,
    val colour: String? = null,
    val pattern: String? = null,
    val formality: Formality? = null,
    val warmth: Warmth? = null,
    val description: String? = null,
)

data class Clothe(
    val id: UUID,
    val userId: UUID,
    val category: ClothingCategory,
    val imageUrl: String,
    val name: String? = null,
    val colour: String? = null,
    val pattern: String? = null,
    val formality: Formality? = null,
    val warmth: Warmth? = null,
    val description: String? = null,
)
