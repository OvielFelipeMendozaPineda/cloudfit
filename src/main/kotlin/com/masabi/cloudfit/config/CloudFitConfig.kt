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

data class S3Config(
    val bucket: String,
    val region: String,
) {
    val configured: Boolean get() = bucket.isNotBlank() && region.isNotBlank()
}

data class DatabaseConfig(
    val host: String,
    val port: String,
    val name: String,
    val user: String,
    val password: String,
) {
    val configured: Boolean get() = host.isNotBlank() && user.isNotBlank() && password.isNotBlank()

    val jdbcUrl: String get() = "jdbc:mysql://$host:$port/$name?sslMode=REQUIRED&connectTimeout=5000"
}

data class RemoveBgConfig(
    val key: String,
) {
    val configured: Boolean get() = key.isNotBlank()
}

data class CloudFitConfig(
    val gemini: GeminiConfig,
    val s3: S3Config,
    val database: DatabaseConfig,
    val removeBg: RemoveBgConfig,
) {
    companion object {
        fun from(config: ApplicationConfig): CloudFitConfig {
            val gemini = config.config("cloudfit.gemini")
            val database = config.config("cloudfit.database")
            val s3 = config.config("cloudfit.s3")
            return CloudFitConfig(
                gemini = GeminiConfig(
                    apiKey = gemini.property("apiKey").getString(),
                    baseUrl = gemini.property("baseUrl").getString(),
                    stylistModel = gemini.property("stylistModel").getString(),
                    taggerModel = gemini.property("taggerModel").getString(),
                    imageModel = gemini.property("imageModel").getString(),
                ),
                s3 = S3Config(
                    bucket = s3.property("bucket").getString(),
                    region = s3.property("region").getString(),
                ),
                database = DatabaseConfig(
                    host = database.property("host").getString(),
                    port = database.property("port").getString(),
                    name = database.property("name").getString(),
                    user = database.property("user").getString(),
                    password = database.property("password").getString(),
                ),
                removeBg = RemoveBgConfig(
                    key = config.property("cloudfit.removeBg.key").getString(),
                ),
            )
        }
    }
}
