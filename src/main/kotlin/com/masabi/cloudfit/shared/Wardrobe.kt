package com.masabi.cloudfit.shared

import kotlinx.serialization.Serializable

/**
 * Clothing categories agreed on for cloud-fit.
 *
 * An outfit must always include a [TOP] + [BOTTOM] + [SHOES], OR a single [DRESS] (which
 * covers both top and bottom). [OUTERWEAR] and [ACCESSORIES] are always optional.
 */
@Serializable
enum class ClothingCategory {
    TOP,
    BOTTOM,
    DRESS,
    SHOES,
    OUTERWEAR,
    ACCESSORIES,
}

/** A single garment owned by the user, stored as an image in S3. */
@Serializable
data class Clothe(
    val id: String,
    val category: ClothingCategory,
    /** Public/pre-signed S3 URL of the garment image. */
    val imageUrl: String,
    val colour: String? = null,
    val description: String? = null,
)

/** A composed look: up to a handful of garments that go together. */
@Serializable
data class Outfit(
    val id: String,
    val clotheIds: List<String>,
    /** S3 URL of the generated avatar-wearing-the-outfit image, once produced. */
    val generatedImageUrl: String? = null,
    /** Short human-readable rationale for the pick. */
    val rationale: String? = null,
)

/** An occasion the user wants an outfit for (e.g. "wedding", "job interview", "casual brunch"). */
@Serializable
data class Event(
    val id: String,
    val name: String,
    val description: String? = null,
)
