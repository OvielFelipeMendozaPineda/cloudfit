package com.masabi.cloudfit.outfit

import com.masabi.cloudfit.shared.Clothe
import com.masabi.cloudfit.shared.Event
import org.slf4j.LoggerFactory

/**
 * Resilience decorator: try [primary]; if it throws (no API key, network, bad/empty response),
 * fall back to [fallback]. Keeps the demo working even when the model is unavailable, without the
 * primary picker having to know about fallbacks (SRP).
 */
class FallbackGarmentPicker(
    private val primary: GarmentPicker,
    private val fallback: GarmentPicker,
) : GarmentPicker {

    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun pick(event: Event, wardrobe: List<Clothe>): Pick =
        try {
            primary.pick(event, wardrobe)
        } catch (e: Exception) {
            log.warn("Primary picker failed ({}), falling back to {}", e.message, fallback::class.simpleName)
            fallback.pick(event, wardrobe)
        }
}
