package app.cloudfit.billing.infrastructure.adapter.input.http

import app.cloudfit.billing.application.port.input.ClaimAdRewardUseCase
import app.cloudfit.billing.application.port.input.CreateCheckoutCommand
import app.cloudfit.billing.application.port.input.CreateCheckoutUseCase
import app.cloudfit.billing.application.port.input.CreatePortalSessionUseCase
import app.cloudfit.billing.application.port.input.GetCatalogUseCase
import app.cloudfit.billing.application.port.input.GetLedgerUseCase
import app.cloudfit.billing.infrastructure.adapter.input.http.dto.AdRewardRequestDto
import app.cloudfit.billing.infrastructure.adapter.input.http.dto.AdRewardResponseDto
import app.cloudfit.billing.infrastructure.adapter.input.http.dto.CatalogDto
import app.cloudfit.billing.infrastructure.adapter.input.http.dto.CheckoutRequestDto
import app.cloudfit.billing.infrastructure.adapter.input.http.dto.CheckoutResponseDto
import app.cloudfit.billing.infrastructure.adapter.input.http.dto.LedgerEntryDto
import app.cloudfit.billing.infrastructure.adapter.input.http.dto.PortalResponseDto
import app.cloudfit.shared.infrastructure.auth.JWT_AUTH
import app.cloudfit.shared.infrastructure.http.requireAuthenticatedUser
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.registerBillingRoutes(
    getCatalog: GetCatalogUseCase,
    getLedger: GetLedgerUseCase,
    createCheckout: CreateCheckoutUseCase,
    createPortalSession: CreatePortalSessionUseCase,
    claimAdReward: ClaimAdRewardUseCase,
) {
    authenticate(JWT_AUTH) {
        route("/billing") {
            get("/catalog") {
                call.requireAuthenticatedUser()
                call.respond(CatalogDto.from(getCatalog.execute(call.country())))
            }
            get("/ledger") {
                val user = call.requireAuthenticatedUser()
                val limit = call.request.queryParameters["limit"]?.toIntOrNull()
                call.respond(getLedger.execute(user.userId, limit).map(LedgerEntryDto::from))
            }
            post("/checkout") {
                val user = call.requireAuthenticatedUser()
                val body = call.receive<CheckoutRequestDto>()
                val url = createCheckout.execute(
                    CreateCheckoutCommand(
                        userId = user.userId,
                        productCode = body.productCode,
                        provider = body.provider,
                        country = call.country(),
                    ),
                )
                call.respond(CheckoutResponseDto(url))
            }
            post("/portal") {
                val user = call.requireAuthenticatedUser()
                call.respond(PortalResponseDto(createPortalSession.execute(user.userId)))
            }
        }
        post("/ads/reward") {
            val user = call.requireAuthenticatedUser()
            val body = call.receive<AdRewardRequestDto>()
            val balance = claimAdReward.execute(user.userId, body.placement)
            call.respond(AdRewardResponseDto(granted = true, balance = balance))
        }
    }
}

private fun ApplicationCall.country(): String? =
    request.queryParameters["country"]?.takeIf { it.isNotBlank() } ?: request.headers["CF-IPCountry"]
