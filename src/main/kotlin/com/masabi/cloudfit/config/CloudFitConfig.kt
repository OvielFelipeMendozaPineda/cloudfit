package com.masabi.cloudfit.config

import io.ktor.server.config.ApplicationConfig

/** Gemini settings — the single AI provider for stylist, tagger and image generation. */
data class GeminiConfig(
    val apiKey: String,
    val baseUrl: String,
    val stylistModel: String,
    val taggerModel: String,
    val imageModel: String,
) {
    /** True when an API key is present. The app requires this to run. */
    val configured: Boolean get() = apiKey.isNotBlank()
}

/** Where generated images are saved. Blank [dir] means the user's Downloads folder. */
data class StorageConfig(
    val dir: String,
) {
    /** The resolved directory, defaulting to ~/Downloads when [dir] is blank. */
    val resolvedDir: String
        get() = dir.ifBlank { "${System.getProperty("user.home")}/Downloads" }
}

/** All app configuration, loaded once from `application.yaml` and passed around as one object. */
data class CloudFitConfig(
    val gemini: GeminiConfig,
    val storage: StorageConfig,
) {
    companion object {
        fun from(config: ApplicationConfig): CloudFitConfig {
            val gemini = config.config("cloudfit.gemini")
            return CloudFitConfig(
                gemini = GeminiConfig(
                    apiKey = gemini.property("apiKey").getString(),
                    baseUrl = gemini.property("baseUrl").getString(),
                    stylistModel = gemini.property("stylistModel").getString(),
                    taggerModel = gemini.property("taggerModel").getString(),
                    imageModel = gemini.property("imageModel").getString(),
                ),
                storage = StorageConfig(
                    dir = config.property("cloudfit.storage.dir").getString(),
                ),
            )
        }
    }
}
