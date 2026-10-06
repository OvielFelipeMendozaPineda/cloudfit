package app.cloudfit.wardrobe.infrastructure.config

import app.cloudfit.wardrobe.domain.WardrobePolicy
import io.ktor.server.config.ApplicationConfig

data class WardrobeConfig(
    val policy: WardrobePolicy,
    val removeBgApiKey: String,
) {
    companion object {
        fun from(config: ApplicationConfig): WardrobeConfig {
            val section = config.config("cloudfit.wardrobe")
            return WardrobeConfig(
                policy = WardrobePolicy(
                    freeMaxClothes = section.property("freeMaxClothes").getString().toInt(),
                    maxUploadBytes = section.propertyOrNull("maxUploadBytes")?.getString()?.toLongOrNull() ?: (10L * 1024 * 1024),
                ),
                removeBgApiKey = config.property("cloudfit.removeBg.apiKey").getString(),
            )
        }
    }
}
