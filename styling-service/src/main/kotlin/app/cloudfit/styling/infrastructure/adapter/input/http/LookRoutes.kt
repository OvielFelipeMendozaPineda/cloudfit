package app.cloudfit.styling.infrastructure.adapter.input.http

import app.cloudfit.shared.infrastructure.auth.JWT_AUTH
import app.cloudfit.shared.infrastructure.http.LOOKS_RATE_LIMIT
import app.cloudfit.shared.infrastructure.http.requireAuthenticatedUser
import app.cloudfit.shared.infrastructure.http.uuidParameter
import app.cloudfit.styling.application.port.input.CreateLookUseCase
import app.cloudfit.styling.application.port.input.DeleteLookUseCase
import app.cloudfit.styling.application.port.input.GetLookUseCase
import app.cloudfit.styling.application.port.input.ListLooksUseCase
import app.cloudfit.styling.application.port.input.SaveLookUseCase
import app.cloudfit.styling.infrastructure.adapter.input.http.dto.CreateLookRequestDto
import app.cloudfit.styling.infrastructure.adapter.input.http.dto.LookDto
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.registerLookRoutes(
    createLook: CreateLookUseCase,
    getLook: GetLookUseCase,
    listLooks: ListLooksUseCase,
    saveLook: SaveLookUseCase,
    deleteLook: DeleteLookUseCase,
) {
    authenticate(JWT_AUTH) {
        route("/looks") {
            rateLimit(LOOKS_RATE_LIMIT) {
                post {
                    val user = call.requireAuthenticatedUser()
                    val look = createLook.execute(user.userId, call.receive<CreateLookRequestDto>().event)
                    call.respond(HttpStatusCode.Accepted, LookDto.from(look))
                }
            }
            get {
                val user = call.requireAuthenticatedUser()
                val savedOnly = call.request.queryParameters["saved"]?.toBooleanStrictOrNull() ?: false
                call.respond(listLooks.execute(user.userId, savedOnly).map(LookDto::from))
            }
            get("/{id}") {
                val user = call.requireAuthenticatedUser()
                call.respond(LookDto.from(getLook.execute(user.userId, call.uuidParameter("id"))))
            }
            post("/{id}/save") {
                val user = call.requireAuthenticatedUser()
                call.respond(LookDto.from(saveLook.execute(user.userId, call.uuidParameter("id"))))
            }
            delete("/{id}") {
                val user = call.requireAuthenticatedUser()
                deleteLook.execute(user.userId, call.uuidParameter("id"))
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
