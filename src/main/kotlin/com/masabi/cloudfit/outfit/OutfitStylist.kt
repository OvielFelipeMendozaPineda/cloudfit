package com.masabi.cloudfit.outfit

import com.masabi.cloudfit.shared.Clothe
import com.masabi.cloudfit.shared.Event
import com.masabi.cloudfit.shared.ImageStatus
import com.masabi.cloudfit.shared.Outfit
import java.util.UUID

/**
 * The AI brain of cloud-fit (Felipe's scope). Orchestrates the pipeline:
 *
 *  STAGE 2  [GarmentPicker]  — pick garments that suit the event and combine well.
 *  STAGE 3  [ImageRenderer]  — render the avatar wearing them (decoupled; may stay PENDING).
 *
 * (STAGE 1, garment tagging on upload, is [GarmentTagger] — called from the closet flow, not here.)
 *
 * Both stages are injected so they can be swapped (real LLM / image model vs. test fakes) and
 * unit-tested without hitting any provider.
 */
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
