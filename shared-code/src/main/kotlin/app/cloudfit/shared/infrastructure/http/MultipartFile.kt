package app.cloudfit.shared.infrastructure.http

import app.cloudfit.shared.application.error.ValidationException
import app.cloudfit.shared.application.port.ImageFormats
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveMultipart
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray

class UploadedFile(
    val bytes: ByteArray,
    val contentType: String,
)

suspend fun ApplicationCall.receiveImageFile(maxBytes: Long): UploadedFile {
    var uploaded: UploadedFile? = null
    var tooLarge = false
    receiveMultipart(formFieldLimit = maxBytes + 1024).forEachPart { part ->
        if (part is PartData.FileItem && uploaded == null) {
            val bytes = part.provider().readRemaining(maxBytes + 1).readByteArray()
            if (bytes.size > maxBytes) {
                tooLarge = true
            } else {
                uploaded = UploadedFile(bytes, part.contentType?.toString() ?: "image/jpeg")
            }
        }
        part.dispose()
    }
    if (tooLarge) throw ValidationException("File exceeds $maxBytes bytes")
    val file = uploaded ?: throw ValidationException("Multipart field 'file' is required")
    if (file.bytes.isEmpty()) throw ValidationException("File is empty")
    if (!ImageFormats.isSupported(file.contentType)) throw ValidationException("Unsupported image type ${file.contentType}")
    return file
}
