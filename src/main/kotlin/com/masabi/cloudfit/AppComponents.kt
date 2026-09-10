package com.masabi.cloudfit

import com.masabi.cloudfit.ai.GeminiClient
import com.masabi.cloudfit.config.CloudFitConfig
import com.masabi.cloudfit.outfit.GarmentTagger
import com.masabi.cloudfit.outfit.GeminiGarmentPicker
import com.masabi.cloudfit.outfit.GeminiGarmentTagger
import com.masabi.cloudfit.outfit.GeminiImageRenderer
import com.masabi.cloudfit.outfit.OutfitStylist
import com.masabi.cloudfit.storage.LocalImageStore
import com.masabi.cloudfit.wardrobe.ClosetRepository
import com.masabi.cloudfit.wardrobe.EventRepository
import com.masabi.cloudfit.wardrobe.InMemoryClosetRepository
import com.masabi.cloudfit.wardrobe.InMemoryEventRepository
import java.nio.file.Path

class AppComponents(
    val closet: ClosetRepository,
    val events: EventRepository,
    val stylist: OutfitStylist,
    val tagger: GarmentTagger,
) {
    companion object {
        fun from(config: CloudFitConfig): AppComponents {
            require(config.gemini.configured) {
                "GEMINI_API_KEY is not set — the app requires Gemini to run."
            }
            val gemini = GeminiClient(config.gemini)

            val picker = GeminiGarmentPicker(gemini, config.gemini.stylistModel)
            val tagger = GeminiGarmentTagger(gemini, config.gemini.taggerModel)

            val imageStore = LocalImageStore(Path.of(config.storage.resolvedDir))
            val renderer = GeminiImageRenderer(gemini, imageStore, config.gemini.imageModel)

            return AppComponents(
                closet = InMemoryClosetRepository(),
                events = InMemoryEventRepository(),
                stylist = OutfitStylist(picker, renderer),
                tagger = tagger,
            )
        }
    }
}
