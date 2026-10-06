package app.cloudfit.wardrobe.application.port.output

import app.cloudfit.wardrobe.domain.ClotheTags

fun interface ClotheTagger {
    suspend fun tag(imageBytes: ByteArray, mimeType: String): ClotheTags
}
