package com.masabi.cloudfit.outfit

import com.masabi.cloudfit.shared.Clothe
import com.masabi.cloudfit.shared.Event
import com.masabi.cloudfit.shared.ImageStatus
import com.masabi.cloudfit.shared.Outfit
import java.util.UUID

class OutfitStylist(
    private val picker: GarmentPicker,
    private val renderer: ImageRenderer = NoopImageRenderer(),
) {

    suspend fun compose(event: Event, wardrobe: List<Clothe>, avatarImageUrl: String? = null): Outfit {
        val pick = picker.pick(event, wardrobe)
        val chosen = wardrobe.filter { it.id in pick.clotheIds.toSet() }

        val render = renderer.render(chosen, avatarImageUrl)
        val status = when {
            render.imageUrl != null -> ImageStatus.READY
            render.failed -> ImageStatus.FAILED
            else -> ImageStatus.PENDING
        }

        return Outfit(
            id = UUID.randomUUID().toString(),
            eventId = event.id,
            clotheIds = pick.clotheIds,
            stylistNote = pick.stylistNote,
            imageStatus = status,
            imageUrl = render.imageUrl,
        )
    }
}
