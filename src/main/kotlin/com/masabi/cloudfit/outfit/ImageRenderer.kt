package com.masabi.cloudfit.outfit

import com.masabi.cloudfit.ai.ImageModel
import com.masabi.cloudfit.ai.InlineData
import com.masabi.cloudfit.ai.Part
import com.masabi.cloudfit.shared.Clothe
import com.masabi.cloudfit.storage.ImageStore
import java.util.Base64
import org.slf4j.LoggerFactory

data class RenderResult(
    val imageUrl: String?,
    val failed: Boolean = false,
)

interface ImageRenderer {
    suspend fun render(garments: List<Clothe>, avatarImageUrl: String?): RenderResult
}

class NoopImageRenderer : ImageRenderer {
    override suspend fun render(garments: List<Clothe>, avatarImageUrl: String?): RenderResult =
        RenderResult(imageUrl = null)
}

class GeminiImageRenderer(
    private val imageModel: ImageModel,
    private val imageStore: ImageStore,
    private val model: String,
) : ImageRenderer {

    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun render(garments: List<Clothe>, avatarImageUrl: String?): RenderResult =
        try {
            val parts = buildList {
                if (avatarImageUrl != null) {
                    val bytes = imageStore.get(avatarImageUrl)
                    add(Part(inlineData = InlineData(mimeOf(avatarImageUrl), Base64.getEncoder().encodeToString(bytes))))
                }
                add(Part(text = prompt(garments, hasPerson = avatarImageUrl != null)))
            }
            val image = imageModel.generateImage(model, parts)
            RenderResult(imageUrl = imageStore.put(image.bytes, image.mimeType))
        } catch (e: Exception) {
            log.warn("Image generation failed ({})", e.message)
            RenderResult(imageUrl = null, failed = true)
        }

    private fun prompt(garments: List<Clothe>, hasPerson: Boolean): String {
        val pieces = garments.joinToString("\n") { c ->
            val desc = listOfNotNull(c.colour, c.pattern, c.description).joinToString(" ")
            "- ${c.category.name.lowercase()}: ${desc.ifBlank { c.name ?: "garment" }}"
        }
        val subject = if (hasPerson) {
            "the person in the first image, keeping their face, body shape and skin tone"
        } else {
            "a person"
        }
        return """
            Generate a full-body studio photograph of $subject wearing this outfit:
            $pieces

            Plain light grey studio background, even natural lighting, photorealistic,
            full body head to feet, centred.
        """.trimIndent()
    }

    private fun mimeOf(url: String): String = when {
        url.endsWith(".png", ignoreCase = true) -> "image/png"
        url.endsWith(".webp", ignoreCase = true) -> "image/webp"
        else -> "image/jpeg"
    }
}
