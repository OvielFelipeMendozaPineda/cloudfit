package app.cloudfit.shared.application.ai

class GeneratedImage(
    val bytes: ByteArray,
    val mimeType: String,
)

interface ImageModel {
    suspend fun generateImage(model: String, parts: List<Part>): GeneratedImage
}
