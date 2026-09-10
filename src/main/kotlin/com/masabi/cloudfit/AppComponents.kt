package com.masabi.cloudfit

import com.masabi.cloudfit.ai.ClothePicker
import com.masabi.cloudfit.ai.ClotheTagger
import com.masabi.cloudfit.ai.GeminiClient
import com.masabi.cloudfit.ai.OutfitImageRenderer
import com.masabi.cloudfit.ai.OutfitStylist
import com.masabi.cloudfit.avatar.AvatarRepository
import com.masabi.cloudfit.bg.BackGroundRemover
import com.masabi.cloudfit.bg.DefaultBackGroundRemover
import com.masabi.cloudfit.clothes.ClothesRepository
import com.masabi.cloudfit.config.CloudFitConfig
import com.masabi.cloudfit.db.Db
import com.masabi.cloudfit.outfit.OutfitsRepository
import com.masabi.cloudfit.storage.ImageStore
import com.masabi.cloudfit.storage.S3ImageStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

class AppComponents(
    val clothes: ClothesRepository,
    val avatars: AvatarRepository,
    val outfits: OutfitsRepository,
    val stylist: OutfitStylist,
    val tagger: ClotheTagger,
    val imageStore: ImageStore,
    val backgroundRemover: BackGroundRemover?,
) {
    companion object {
        fun from(config: CloudFitConfig): AppComponents {
            require(config.gemini.configured) {
                "GEMINI_API_KEY is not set — the app requires Gemini to run."
            }
            require(config.database.configured) {
                "DB_HOST/DB_USER/DB_PASSWORD are not set — the app requires RDS to run."
            }
            require(config.s3.configured) {
                "AWS_S3_BUCKET/AWS_REGION are not set — the app requires S3 to run."
            }
            Db.connect(config.database)

            val gemini = GeminiClient(config.gemini)

            val picker = ClothePicker(gemini, config.gemini.stylistModel)
            val tagger = ClotheTagger(gemini, config.gemini.taggerModel)

            val imageStore = S3ImageStore(config.s3)
            val renderer = OutfitImageRenderer(gemini, imageStore, config.gemini.imageModel)

            val backgroundRemover = if (config.removeBg.configured) {
                DefaultBackGroundRemover(config.removeBg.key, HttpClient(CIO))
            } else {
                null
            }

            return AppComponents(
                clothes = ClothesRepository(),
                avatars = AvatarRepository(),
                outfits = OutfitsRepository(),
                stylist = OutfitStylist(picker, renderer),
                tagger = tagger,
                imageStore = imageStore,
                backgroundRemover = backgroundRemover,
            )
        }
    }
}
