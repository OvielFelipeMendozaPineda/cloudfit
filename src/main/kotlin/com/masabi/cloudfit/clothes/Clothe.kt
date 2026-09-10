package com.masabi.cloudfit.clothes

import kotlinx.serialization.Serializable

@Serializable
enum class ClothingCategory { TOP, BOTTOM, DRESS, SHOES, OUTERWEAR, ACCESSORIES }

@Serializable
enum class Formality { CASUAL, SMART_CASUAL, FORMAL }

@Serializable
enum class Warmth { LIGHT, MEDIUM, WARM }

@Serializable
enum class ClotheSource { UPLOAD, CATALOG }

@Serializable
data class Clothe(
    val id: String,
    val category: ClothingCategory,
    val imageUrl: String,
    val sourceImageUrl: String? = null,
    val name: String? = null,
    val colour: String? = null,
    val pattern: String? = null,
    val formality: Formality? = null,
    val warmth: Warmth? = null,
    val description: String? = null,
    val source: ClotheSource = ClotheSource.UPLOAD,
)
