package com.masabi.cloudfit

import com.masabi.cloudfit.ai.GeminiClient
import com.masabi.cloudfit.config.CloudFitConfig
import com.masabi.cloudfit.outfit.FallbackGarmentPicker
import com.masabi.cloudfit.outfit.GeminiGarmentPicker
import com.masabi.cloudfit.outfit.OutfitStylist
import com.masabi.cloudfit.outfit.RuleBasedPicker
import com.masabi.cloudfit.wardrobe.ClosetRepository
import com.masabi.cloudfit.wardrobe.EventRepository
import com.masabi.cloudfit.wardrobe.InMemoryClosetRepository
import com.masabi.cloudfit.wardrobe.InMemoryEventRepository

/**
 * Composition root: the one place that decides which concrete implementations to wire together.
 * Everything else depends on abstractions, so this is the only file that changes when we swap a
 * provider or a persistence backend.
 */
class AppComponents(
    val closet: ClosetRepository,
    val events: EventRepository,
    val stylist: OutfitStylist,
) {
    companion object {
        fun from(config: CloudFitConfig): AppComponents {
            val rules = RuleBasedPicker()
            val picker = if (config.gemini.configured) {
                // Real stylist, with the rules picker as a resilience fallback.
                FallbackGarmentPicker(
                    primary = GeminiGarmentPicker(GeminiClient(config.gemini), config.gemini.stylistModel),
                    fallback = rules,
                )
            } else {
                rules
            }

            return AppComponents(
                closet = InMemoryClosetRepository(),
                events = InMemoryEventRepository(),
                stylist = OutfitStylist(picker),
            )
        }
    }
}
