package com.masabi.cloudfit.config

import io.ktor.server.config.ApplicationConfig

data class GeminiConfig(
    val apiKey: String,
    val baseUrl: String,
    val stylistModel: String,
    val taggerModel: String,
    val imageModel: String,
) {
    val configured: Boolean get() = apiKey.isNotBlank()
}

data class StorageConfig(
    val dir: String,
) {
    val resolvedDir: String
        get() = dir.ifBlank { "${System.getProperty("user.home")}/Downloads" }
}

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
