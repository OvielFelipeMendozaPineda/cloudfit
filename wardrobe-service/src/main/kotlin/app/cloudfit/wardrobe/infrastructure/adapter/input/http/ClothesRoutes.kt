package app.cloudfit.wardrobe.infrastructure.adapter.input.http

import app.cloudfit.shared.infrastructure.http.requireAuthenticatedUser
import app.cloudfit.shared.infrastructure.http.uuidParameter
import app.cloudfit.wardrobe.application.port.input.CreateClotheUseCase
import app.cloudfit.wardrobe.application.port.input.DeleteClotheUseCase
import app.cloudfit.wardrobe.application.port.input.ListClothesUseCase
import app.cloudfit.wardrobe.application.port.input.UpdateClotheUseCase
import app.cloudfit.wardrobe.infrastructure.adapter.input.http.dto.ClotheDto
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.registerClothesRoutes(
    listClothes: ListClothesUseCase,
    createClothe: CreateClotheUseCase,
    updateClothe: UpdateClotheUseCase,
    deleteClothe: DeleteClotheUseCase,
) {
    route("/clothes") {
        get {
            val user = call.requireAuthenticatedUser()
            call.respond(listClothes.execute(user.userId).map(ClotheDto::from))
        }
        post {
            val user = call.requireAuthenticatedUser()
            val clothe = createClothe.execute(user.userId, call.receive<ClotheDto>().toDraft())
            call.respond(HttpStatusCode.Created, ClotheDto.from(clothe))
        }
        put("/{id}") {
            val user = call.requireAuthenticatedUser()
            val id = call.uuidParameter("id")
            val clothe = updateClothe.execute(user.userId, id, call.receive<ClotheDto>().toDraft())
            call.respond(ClotheDto.from(clothe))
        }
        delete("/{id}") {
            val user = call.requireAuthenticatedUser()
            deleteClothe.execute(user.userId, call.uuidParameter("id"))
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
