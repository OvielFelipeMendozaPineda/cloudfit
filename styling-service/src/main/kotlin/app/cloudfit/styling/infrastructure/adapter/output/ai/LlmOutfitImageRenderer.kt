package app.cloudfit.styling.infrastructure.adapter.output.ai

import app.cloudfit.shared.application.ai.GeneratedImage
import app.cloudfit.shared.application.ai.ImageModel
import app.cloudfit.shared.application.ai.InlineData
import app.cloudfit.shared.application.ai.Part
import app.cloudfit.shared.application.port.ImageStore
import app.cloudfit.styling.application.port.output.OutfitImageRenderer
import app.cloudfit.styling.domain.WardrobeItem
import java.util.Base64

class LlmOutfitImageRenderer(
    private val imageModel: ImageModel,
    private val imageStore: ImageStore,
    private val model: String,
) : OutfitImageRenderer {

    override suspend fun render(clothes: List<WardrobeItem>, avatarImageUrl: String?): GeneratedImage {
        val parts = buildList {
            if (avatarImageUrl != null) add(inline(avatarImageUrl))
            clothes.forEach { add(inline(it.imageUrl)) }
            add(Part(text = prompt(clothes, hasPerson = avatarImageUrl != null)))
        }
        return imageModel.generateImage(model, parts)
    }

    private suspend fun inline(url: String): Part {
        val image = imageStore.read(url)
        return Part(inlineData = InlineData(image.contentType, Base64.getEncoder().encodeToString(image.bytes)))
    }

    private fun prompt(clothes: List<WardrobeItem>, hasPerson: Boolean): String {
        val firstClotheImageIndex = if (hasPerson) 2 else 1
        val pieces = clothes.mapIndexed { i, c ->
            val desc = listOfNotNull(c.colour, c.pattern, c.description).joinToString(" ")
            "- ${c.category.name.lowercase()} (reference image ${firstClotheImageIndex + i}): ${desc.ifBlank { c.name ?: "clothe" }}"
        }.joinToString("\n")
        val subject = if (hasPerson) {
            "the exact person in the first image — same face, body shape, skin tone, and pose, unchanged"
        } else {
            "a person"
        }
        return """
        Generate a full-body studio photograph of $subject wearing EXACTLY this outfit and nothing else, reproducing each garment exactly as shown in its reference image (same color, pattern, cut and design):
        $pieces

        Do not change the pose, face, or body from the reference image. Only replace the clothing.
        Plain light grey studio background, even natural lighting, photorealistic,
        full body head to feet, centred.
        Output a 1024x1536 image, 2:3 portrait aspect ratio, sharp focus, no cropping of the head or feet.
    """.trimIndent()
    }
}
