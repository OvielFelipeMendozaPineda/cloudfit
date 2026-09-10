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

/** How dressy a garment is. Enumerated to keep Claude's tagging output consistent. */
@Serializable
enum class Formality { CASUAL, SMART_CASUAL, FORMAL }

/** How warm a garment is. */
@Serializable
enum class Warmth { LIGHT, MEDIUM, WARM }

/** Where the garment came from. */
@Serializable
enum class GarmentSource { UPLOAD, CATALOG }

/**
 * A single garment owned by the user.
 *
 * [imageUrl] is the processed image (background removed) shown in the closet; [sourceImageUrl] is
 * the original upload, kept so background removal can be retried. The tag fields ([colour],
 * [pattern], [formality], [warmth], [description]) are filled in by the [GarmentTagger] (stage 1).
 */
@Serializable
data class Clothe(
    val id: String,
    val category: ClothingCategory,
    /** Processed (background-removed) S3 URL shown in the closet. */
    val imageUrl: String,
    /** Original uploaded image, kept so background removal can be retried. */
    val sourceImageUrl: String? = null,
    val name: String? = null,
    val colour: String? = null,
    val pattern: String? = null,
    val formality: Formality? = null,
    val warmth: Warmth? = null,
    val description: String? = null,
    val source: GarmentSource = GarmentSource.UPLOAD,
)

/** Whether the generated look image is ready yet (image gen is decoupled from outfit assembly). */
@Serializable
enum class ImageStatus { PENDING, READY, FAILED }

/**
 * A composed look for an event: the garments that go together, a short stylist note, and — once
 * the image model has run — the rendered image of the avatar wearing it.
 */
@Serializable
data class Outfit(
    val id: String,
    val eventId: String,
    val clotheIds: List<String>,
    /** One or two short sentences explaining the pick. */
    val stylistNote: String? = null,
    val imageStatus: ImageStatus = ImageStatus.PENDING,
    /** S3 URL of the generated avatar-wearing-the-outfit image, once produced. */
    val imageUrl: String? = null,
)

/** An occasion the user wants an outfit for (e.g. "wedding", "job interview", "casual brunch"). */
@Serializable
data class Event(
    val id: String,
    val name: String,
    val description: String? = null,
)
