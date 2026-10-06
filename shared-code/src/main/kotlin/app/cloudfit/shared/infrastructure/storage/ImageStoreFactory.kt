package app.cloudfit.shared.infrastructure.storage

import app.cloudfit.shared.application.port.ImageStore
import java.nio.file.Path

object ImageStoreFactory {
    fun create(config: ImageStoreConfig): ImageStore = when (config.kind) {
        ImageStoreKind.LOCAL -> LocalImageStore(Path.of(config.localDir), config.publicPath)
        ImageStoreKind.S3 -> S3ImageStore(config.s3Bucket, config.s3Region)
    }
}
