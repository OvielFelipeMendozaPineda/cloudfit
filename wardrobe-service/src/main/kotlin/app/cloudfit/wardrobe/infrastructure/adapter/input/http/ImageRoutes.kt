package app.cloudfit.wardrobe.infrastructure.adapter.input.http

import app.cloudfit.shared.application.error.NotFoundException
import app.cloudfit.shared.infrastructure.http.receiveImageFile
import app.cloudfit.shared.infrastructure.http.requireAuthenticatedUser
import app.cloudfit.wardrobe.application.port.input.GetImageUseCase
import app.cloudfit.wardrobe.application.port.input.RemoveBackgroundUseCase
import app.cloudfit.wardrobe.application.port.input.TagClotheUseCase
import app.cloudfit.wardrobe.application.port.input.UploadImageUseCase
import app.cloudfit.wardrobe.infrastructure.adapter.input.http.dto.ClotheTagsDto
import app.cloudfit.wardrobe.infrastructure.adapter.input.http.dto.PhotoUrlDto
import app.cloudfit.wardrobe.infrastructure.adapter.input.http.dto.UploadResponseDto
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.registerImageRoutes(
    uploadImage: UploadImageUseCase,
    removeBackground: RemoveBackgroundUseCase,
    tagClothe: TagClotheUseCase,
    maxUploadBytes: Long,
) {
    post("/uploads") {
        val user = call.requireAuthenticatedUser()
        val file = call.receiveImageFile(maxUploadBytes)
        val url = uploadImage.execute(user.userId, file.bytes, file.contentType)
        call.respond(HttpStatusCode.Created, UploadResponseDto(url))
    }
    post("/remove-bg") {
        val user = call.requireAuthenticatedUser()
        val file = call.receiveImageFile(maxUploadBytes)
        call.respond(PhotoUrlDto(removeBackground.execute(user.userId, file.bytes, file.contentType)))
    }
    post("/tagger") {
        val user = call.requireAuthenticatedUser()
        val file = call.receiveImageFile(maxUploadBytes)
        call.respond(ClotheTagsDto.from(tagClothe.execute(user.userId, file.bytes, file.contentType)))
    }
}

fun Route.registerPublicImageRoutes(getImage: GetImageUseCase) {
    get("/images/{key}") {
        val image = getImage.execute(call.parameters["key"].orEmpty()) ?: throw NotFoundException("Image not found")
        call.response.header(HttpHeaders.CacheControl, "public, max-age=31536000, immutable")
        call.respondBytes(image.bytes, ContentType.parse(image.contentType))
    }
}
