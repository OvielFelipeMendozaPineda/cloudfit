package app.cloudfit.wardrobe.infrastructure.adapter.input.http.dto

import app.cloudfit.shared.domain.ClothingCategory
import app.cloudfit.shared.domain.Formality
import app.cloudfit.shared.domain.Warmth
import app.cloudfit.wardrobe.domain.Clothe
import app.cloudfit.wardrobe.domain.ClotheDraft
import app.cloudfit.wardrobe.domain.ClotheTags
import app.cloudfit.wardrobe.domain.DefaultClothe
import kotlinx.serialization.Serializable

@Serializable
data class ClotheDto(
    val id: String? = null,
    val category: ClothingCategory,
    val imageUrl: String,
    val name: String? = null,
    val colour: String? = null,
    val pattern: String? = null,
    val formality: Formality? = null,
    val warmth: Warmth? = null,
    val description: String? = null,
) {
    fun toDraft() = ClotheDraft(category, imageUrl, name, colour, pattern, formality, warmth, description)

    companion object {
        fun from(clothe: Clothe) = ClotheDto(
            id = clothe.id.toString(),
            category = clothe.category,
            imageUrl = clothe.imageUrl,
            name = clothe.name,
            colour = clothe.colour,
            pattern = clothe.pattern,
            formality = clothe.formality,
            warmth = clothe.warmth,
            description = clothe.description,
        )

        fun from(clothe: DefaultClothe) = ClotheDto(
            id = clothe.id,
            category = clothe.category,
            imageUrl = clothe.imageUrl,
            name = clothe.name,
            colour = clothe.colour,
        )
    }
}

@Serializable
data class ClotheTagsDto(
    val name: String?,
    val category: ClothingCategory,
    val colour: String?,
    val pattern: String?,
    val formality: Formality?,
    val warmth: Warmth?,
    val description: String?,
) {
    companion object {
        fun from(tags: ClotheTags) = ClotheTagsDto(
            tags.name,
            tags.category,
            tags.colour,
            tags.pattern,
            tags.formality,
            tags.warmth,
            tags.description,
        )
    }
}

@Serializable
data class UploadResponseDto(val url: String)

@Serializable
data class PhotoUrlDto(val photoUrl: String = "")
