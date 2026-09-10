package com.masabi.cloudfit.outfit

import com.masabi.cloudfit.shared.Clothe

/** Outcome of stage 3: either a rendered image URL, or a failure the frontend can shimmer over. */
data class RenderResult(
    val imageUrl: String?,
    val failed: Boolean = false,
)

/**
 * STAGE 3 of the AI pipeline — composite the chosen garments onto the user's body photo.
 *
 * This is decoupled from garment picking on purpose: assembly returns in 2–4s, rendering takes
 * 8–15s, so the frontend shows the stylist note immediately and shimmers while the image loads.
 *
 *  - [NoopImageRenderer]  : returns nothing (image stays PENDING). Used until Gemini is wired in.
 *  - GeminiImageRenderer  : TODO(felipe) — call the vision model, upload the result to S3.
 */
interface ImageRenderer {
    suspend fun render(garments: List<Clothe>, avatarImageUrl: String?): RenderResult
}

/** Placeholder: no image yet. Keeps the outfit flow working end-to-end without an image model. */
class NoopImageRenderer : ImageRenderer {
    override suspend fun render(garments: List<Clothe>, avatarImageUrl: String?): RenderResult =
        RenderResult(imageUrl = null)
}
