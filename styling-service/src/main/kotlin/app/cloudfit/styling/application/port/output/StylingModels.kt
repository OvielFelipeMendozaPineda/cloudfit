package app.cloudfit.styling.application.port.output

import app.cloudfit.shared.application.ai.GeneratedImage
import app.cloudfit.shared.domain.AppLocale
import app.cloudfit.styling.domain.Pick
import app.cloudfit.styling.domain.WardrobeItem

fun interface ClothePicker {
    suspend fun pick(event: String, wardrobe: List<WardrobeItem>, locale: AppLocale): Pick
}

fun interface OutfitImageRenderer {
    suspend fun render(clothes: List<WardrobeItem>, avatarImageUrl: String?): GeneratedImage
}
