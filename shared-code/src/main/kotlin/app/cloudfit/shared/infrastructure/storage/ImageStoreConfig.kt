package app.cloudfit.shared.infrastructure.storage

import io.ktor.server.config.ApplicationConfig

enum class ImageStoreKind { LOCAL, S3 }

data class ImageStoreConfig(
    val kind: ImageStoreKind,
    val localDir: String,
    val publicPath: String,
    val s3Bucket: String,
    val s3Region: String,
) {
    companion object {
        fun from(config: ApplicationConfig): ImageStoreConfig {
            val section = config.config("cloudfit.images")
            val kind = section.property("store").getString().trim().uppercase()
            return ImageStoreConfig(
                kind = ImageStoreKind.entries.firstOrNull { it.name == kind }
                    ?: error("IMAGE_STORE must be 'local' or 's3', got '$kind'"),
                localDir = section.property("localDir").getString(),
                publicPath = section.propertyOrNull("publicPath")?.getString() ?: "/api/v1/images",
                s3Bucket = section.property("s3Bucket").getString(),
                s3Region = section.property("s3Region").getString(),
            )
        }
    }
}
