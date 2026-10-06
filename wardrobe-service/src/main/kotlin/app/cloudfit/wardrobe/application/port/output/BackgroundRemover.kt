package app.cloudfit.wardrobe.application.port.output

fun interface BackgroundRemover {
    suspend fun remove(image: ByteArray, contentType: String): ByteArray
}
