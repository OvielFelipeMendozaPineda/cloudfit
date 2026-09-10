package com.masabi.cloudfit.shared

import kotlinx.serialization.Serializable

@Serializable
enum class ClothingCategory {
    TOP,
    BOTTOM,
    DRESS,
    SHOES,
    OUTERWEAR,
    ACCESSORIES,
}

@Serializable
enum class Formality { CASUAL, SMART_CASUAL, FORMAL }

@Serializable
enum class Warmth { LIGHT, MEDIUM, WARM }

@Serializable
enum class GarmentSource { UPLOAD, CATALOG }

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
    val source: GarmentSource = GarmentSource.UPLOAD,
)

@Serializable
enum class ImageStatus { PENDING, READY, FAILED }

@Serializable
data class Outfit(
    val id: String,
    val eventId: String,
    val clotheIds: List<String>,
    val stylistNote: String? = null,
    val imageStatus: ImageStatus = ImageStatus.PENDING,
    val imageUrl: String? = null,
)

@Serializable
data class Event(
    val id: String,
    val name: String,
    val description: String? = null,
)
