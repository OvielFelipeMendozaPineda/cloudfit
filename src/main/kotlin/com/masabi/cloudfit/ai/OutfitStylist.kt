package com.masabi.cloudfit.ai

import com.masabi.cloudfit.clothes.Clothe
import com.masabi.cloudfit.outfit.ImageStatus
import com.masabi.cloudfit.outfit.Outfit
import java.util.UUID

class OutfitStylist(
    private val picker: ClothePicker,
    private val renderer: OutfitImageRenderer,
) {

    suspend fun pick(event: String, wardrobe: List<Clothe>): Outfit {
        val pick = picker.pick(event, wardrobe)
        return Outfit(
            id = UUID.randomUUID().toString(),
            event = event,
            clotheIds = pick.clotheIds,
            stylistNote = pick.stylistNote,
            imageStatus = ImageStatus.PENDING,
            imageUrl = null,
        )
    }

    suspend fun renderImage(outfit: Outfit, wardrobe: List<Clothe>, avatarImageUrl: String?): Outfit {
        val chosen = wardrobe.filter { it.id in outfit.clotheIds.toSet() }
        val render = renderer.render(chosen, avatarImageUrl)
        val status = if (render.imageUrl != null) ImageStatus.READY else ImageStatus.FAILED
        return outfit.copy(imageStatus = status, imageUrl = render.imageUrl)
    }
}
