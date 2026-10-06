package app.cloudfit.wardrobe.infrastructure.adapter.input.http

import app.cloudfit.wardrobe.domain.DefaultCatalog
import app.cloudfit.wardrobe.infrastructure.adapter.input.http.dto.ClotheDto
import app.cloudfit.wardrobe.infrastructure.adapter.input.http.dto.PhotoUrlDto
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.registerDefaultsRoutes() {
    route("/default") {
        get("/clothes") { call.respond(DefaultCatalog.clothes.map(ClotheDto::from)) }
        get("/avatars") { call.respond(DefaultCatalog.avatars.map(::PhotoUrlDto)) }
    }
}
