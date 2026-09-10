package com.masabi.cloudfit.outfit

import com.masabi.cloudfit.shared.Clothe
import com.masabi.cloudfit.shared.Event

data class Pick(
    val clotheIds: List<String>,
    val stylistNote: String,
)

interface GarmentPicker {
    suspend fun pick(event: Event, wardrobe: List<Clothe>): Pick
}
