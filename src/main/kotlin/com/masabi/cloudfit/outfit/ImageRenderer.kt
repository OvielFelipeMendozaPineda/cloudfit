package com.masabi.cloudfit.outfit

import com.masabi.cloudfit.ai.ImageModel
import com.masabi.cloudfit.ai.Part
import com.masabi.cloudfit.shared.Clothe
import com.masabi.cloudfit.storage.ImageStore
import org.slf4j.LoggerFactory

/** Outcome of stage 3: either a rendered image URL, or a failure the frontend can shimmer over. */
data class RenderResult(
    val imageUrl: String?,
    val failed: Boolean = false,
)

/**
 * STAGE 3 of the AI pipeline — produce the look image.
 *
 *  - [NoopImageRenderer]   : returns nothing (image stays PENDING).
 *  - [GeminiImageRenderer] : generates the image with Gemini and hands the bytes to an
 *                            [ImageStore] to persist (local disk now, S3 later).
 */
interface ImageRenderer {
    suspend fun render(garments: List<Clothe>, avatarImageUrl: String?): RenderResult
}

/** Placeholder: no image yet. Keeps the outfit flow working end-to-end without an image model. */
class NoopImageRenderer : ImageRenderer {
    override suspend fun render(garments: List<Clothe>, avatarImageUrl: String?): RenderResult =
        RenderResult(imageUrl = null)
}

/**
 * Generates the look image with Gemini and saves it via [imageStore].
 *
 * Generation and storage are separate concerns: this class only builds the prompt and calls the
 * model; where the bytes end up (Downloads, S3, ...) is entirely decided by which [ImageStore] is
 * injected. A failure returns `failed = true` so the look falls back to the collage + note.
 */
class GeminiImageRenderer(
    private val imageModel: ImageModel,
    private val imageStore: ImageStore,
    private val model: String,
) : ImageRenderer {

    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun render(garments: List<Clothe>, avatarImageUrl: String?): RenderResult =
        try {
            val image = imageModel.generateImage(model, listOf(Part(text = prompt(garments))))
            RenderResult(imageUrl = imageStore.put(image.bytes, image.mimeType))
        } catch (e: Exception) {
            log.warn("Image generation failed ({})", e.message)
            RenderResult(imageUrl = null, failed = true)
        }

    private fun prompt(garments: List<Clothe>): String {
        val pieces = garments.joinToString("\n") { c ->
            val desc = listOfNotNull(c.colour, c.pattern, c.description).joinToString(" ")
            "- ${c.category.name.lowercase()}: ${desc.ifBlank { c.name ?: "garment" }}"
        }
        return """
            Generate a full-body studio photograph of a person wearing this outfit:
            $pieces

            Plain light grey studio background, even natural lighting, photorealistic,
            full body head to feet, centred.
        """.trimIndent()
    }
}
