package com.masabi.cloudfit

import com.masabi.cloudfit.ai.ClothePicker
import com.masabi.cloudfit.ai.ClotheTagger
import com.masabi.cloudfit.ai.GeminiClient
import com.masabi.cloudfit.ai.OutfitImageRenderer
import com.masabi.cloudfit.ai.OutfitStylist
import com.masabi.cloudfit.avatar.AvatarRepository
import com.masabi.cloudfit.clothes.ClothesRepository
import com.masabi.cloudfit.config.CloudFitConfig
import com.masabi.cloudfit.outfit.OutfitsRepository
import com.masabi.cloudfit.storage.LocalImageStore
import java.nio.file.Path

class AppComponents(
    val clothes: ClothesRepository,
    val avatars: AvatarRepository,
    val outfits: OutfitsRepository,
    val stylist: OutfitStylist,
    val tagger: ClotheTagger,
) {
    companion object {
        fun from(config: CloudFitConfig): AppComponents {
            require(config.gemini.configured) {
                "GEMINI_API_KEY is not set — the app requires Gemini to run."
            }
            val gemini = GeminiClient(config.gemini)

            val picker = ClothePicker(gemini, config.gemini.stylistModel)
            val tagger = ClotheTagger(gemini, config.gemini.taggerModel)

            val imageStore = LocalImageStore(Path.of(config.storage.resolvedDir))
            val renderer = OutfitImageRenderer(gemini, imageStore, config.gemini.imageModel)

            return AppComponents(
                clothes = ClothesRepository(),
                avatars = AvatarRepository(),
                outfits = OutfitsRepository(),
                stylist = OutfitStylist(picker, renderer),
                tagger = tagger,
            )
        }
    }
}
