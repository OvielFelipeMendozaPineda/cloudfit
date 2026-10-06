package app.cloudfit.wardrobe.infrastructure.adapter.input.http

import app.cloudfit.shared.application.error.NotFoundException
import app.cloudfit.shared.infrastructure.http.requireAuthenticatedUser
import app.cloudfit.wardrobe.application.port.input.DeleteAvatarUseCase
import app.cloudfit.wardrobe.application.port.input.GetAvatarUseCase
import app.cloudfit.wardrobe.application.port.input.SetAvatarUseCase
import app.cloudfit.wardrobe.infrastructure.adapter.input.http.dto.PhotoUrlDto
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.registerAvatarRoutes(
    getAvatar: GetAvatarUseCase,
    setAvatar: SetAvatarUseCase,
    deleteAvatar: DeleteAvatarUseCase,
) {
    route("/avatar") {
        get {
            val user = call.requireAuthenticatedUser()
            val url = getAvatar.execute(user.userId) ?: throw NotFoundException("No avatar")
            call.respond(PhotoUrlDto(url))
        }
        put {
            val user = call.requireAuthenticatedUser()
            call.respond(PhotoUrlDto(setAvatar.execute(user.userId, call.receive<PhotoUrlDto>().photoUrl)))
        }
        delete {
            val user = call.requireAuthenticatedUser()
            deleteAvatar.execute(user.userId)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
