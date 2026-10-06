package app.cloudfit.shared.infrastructure.ai

import io.ktor.server.config.ApplicationConfig

data class GeminiConfig(
    val apiKey: String,
    val baseUrl: String,
    val stylistModel: String,
    val taggerModel: String,
    val imageModel: String,
) {
    val configured: Boolean get() = apiKey.isNotBlank()

    companion object {
        fun from(config: ApplicationConfig): GeminiConfig {
            val section = config.config("cloudfit.gemini")
            return GeminiConfig(
                apiKey = section.property("apiKey").getString(),
                baseUrl = section.property("baseUrl").getString(),
                stylistModel = section.property("stylistModel").getString(),
                taggerModel = section.property("taggerModel").getString(),
                imageModel = section.property("imageModel").getString(),
            )
        }
    }
}
