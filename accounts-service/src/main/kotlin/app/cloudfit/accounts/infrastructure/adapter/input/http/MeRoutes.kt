package app.cloudfit.accounts.infrastructure.adapter.input.http

import app.cloudfit.accounts.application.port.input.DeleteAccountUseCase
import app.cloudfit.accounts.application.port.input.GetMeUseCase
import app.cloudfit.accounts.application.port.input.UpdateMeCommand
import app.cloudfit.accounts.application.port.input.UpdateMeUseCase
import app.cloudfit.accounts.infrastructure.adapter.input.http.dto.MeDto
import app.cloudfit.accounts.infrastructure.adapter.input.http.dto.UpdateMeRequestDto
import app.cloudfit.shared.infrastructure.auth.JWT_AUTH
import app.cloudfit.shared.infrastructure.http.requireAuthenticatedUser
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.route

fun Route.registerMeRoutes(
    getMe: GetMeUseCase,
    updateMe: UpdateMeUseCase,
    deleteAccount: DeleteAccountUseCase,
    cookie: RefreshCookieSettings,
) {
    authenticate(JWT_AUTH) {
        route("/me") {
            get {
                val user = call.requireAuthenticatedUser()
                call.respond(MeDto.from(getMe.execute(user.userId)))
            }
            patch {
                val user = call.requireAuthenticatedUser()
                val body = call.receive<UpdateMeRequestDto>()
                call.respond(MeDto.from(updateMe.execute(UpdateMeCommand(user.userId, body.displayName, body.locale))))
            }
            delete {
                val user = call.requireAuthenticatedUser()
                deleteAccount.execute(user.userId)
                call.clearRefreshCookie(cookie)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
