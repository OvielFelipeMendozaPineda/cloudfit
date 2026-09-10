package com.masabi.cloudfit.outfit

import com.masabi.cloudfit.shared.Clothe
import com.masabi.cloudfit.shared.Event

/** Result of stage 2: which garments to wear, plus a short stylist note. */
data class Pick(
    val clotheIds: List<String>,
    val stylistNote: String,
)

/**
 * STAGE 2 of the AI pipeline — choose the garments that suit the event and combine well.
 *
 * Backed by an LLM ([GeminiGarmentPicker]).
 */
interface GarmentPicker {
    suspend fun pick(event: Event, wardrobe: List<Clothe>): Pick
}
