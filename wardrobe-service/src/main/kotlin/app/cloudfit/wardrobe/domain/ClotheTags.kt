package app.cloudfit.wardrobe.domain

import app.cloudfit.shared.domain.ClothingCategory
import app.cloudfit.shared.domain.Formality
import app.cloudfit.shared.domain.Warmth

data class ClotheTags(
    val name: String?,
    val category: ClothingCategory,
    val colour: String?,
    val pattern: String?,
    val formality: Formality?,
    val warmth: Warmth?,
    val description: String?,
)
