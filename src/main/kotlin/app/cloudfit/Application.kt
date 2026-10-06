package app.cloudfit

import app.cloudfit.accounts.application.port.output.AccountDataEraser
import app.cloudfit.accounts.infrastructure.config.AccountsComponents
import app.cloudfit.accounts.infrastructure.config.AccountsConfig
import app.cloudfit.accounts.infrastructure.config.configureAccountsServiceRoutes
import app.cloudfit.billing.application.port.output.BillingUserDirectory
import app.cloudfit.billing.infrastructure.config.BillingComponents
import app.cloudfit.billing.infrastructure.config.BillingConfig
import app.cloudfit.billing.infrastructure.config.configureBillingServiceRoutes
import app.cloudfit.config.AppConfig
import app.cloudfit.shared.infrastructure.ai.GeminiClient
import app.cloudfit.shared.infrastructure.ai.GeminiConfig
import app.cloudfit.shared.infrastructure.ai.PostgresAiUsageRecorder
import app.cloudfit.shared.infrastructure.auth.JwtSettings
import app.cloudfit.shared.infrastructure.auth.configureJwtAuth
import app.cloudfit.shared.infrastructure.database.DatabaseConfig
import app.cloudfit.shared.infrastructure.database.DatabaseFactory
import app.cloudfit.shared.infrastructure.database.ExposedTransactionRunner
import app.cloudfit.shared.infrastructure.http.configureApiStatusPages
import app.cloudfit.shared.infrastructure.http.configureRateLimits
import app.cloudfit.shared.infrastructure.storage.ImageStoreConfig
import app.cloudfit.shared.infrastructure.storage.ImageStoreFactory
import app.cloudfit.shared.infrastructure.time.SystemClockProvider
import app.cloudfit.styling.infrastructure.config.StylingComponents
import app.cloudfit.styling.infrastructure.config.StylingModels
import app.cloudfit.styling.infrastructure.config.configureStylingServiceRoutes
import app.cloudfit.wardrobe.infrastructure.config.WardrobeComponents
import app.cloudfit.wardrobe.infrastructure.config.WardrobeConfig
import app.cloudfit.wardrobe.infrastructure.config.configureWardrobeServiceRoutes
import app.cloudfit.wiring.AccountBalanceAdapter
import app.cloudfit.wiring.AvatarUrlAdapter
import app.cloudfit.wiring.CreditWalletAdapter
import app.cloudfit.wiring.PlanStatusAdapter
import app.cloudfit.wiring.RenderedImageSaverAdapter
import app.cloudfit.wiring.UserLocaleAdapter
import app.cloudfit.wiring.WardrobeLimitsAdapter
import app.cloudfit.wiring.WardrobeReaderAdapter
import app.cloudfit.wiring.WelcomeBonusAdapter
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.Url
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.auth.Authentication
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.event.Level

fun main(args: Array<String>) = EngineMain.main(args)

@Serializable
data class HealthDto(val status: String = "ok")

fun Application.module() {
    val config = environment.config
    val appConfig = AppConfig.from(config)
    val jwtSettings = JwtSettings.from(config)
    val dataSource = DatabaseFactory.connect(DatabaseConfig.from(config))

    val appScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, e -> log.error("Background job failed", e) },
    )
    val http = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 10_000
        }
    }
    monitor.subscribe(ApplicationStopping) { appScope.cancel() }
    monitor.subscribe(ApplicationStopped) {
        http.close()
        dataSource.close()
    }

    val clock = SystemClockProvider()
    val tx = ExposedTransactionRunner()
    val geminiConfig = GeminiConfig.from(config)
    if (!geminiConfig.configured) log.warn("GEMINI_API_KEY is not set: tagging and looks will fail with AI_UNAVAILABLE")
    val gemini = GeminiClient(geminiConfig, PostgresAiUsageRecorder(clock))
    val imageStore = ImageStoreFactory.create(ImageStoreConfig.from(config))

    lateinit var accounts: AccountsComponents
    val billing = BillingComponents(
        config = BillingConfig.from(config, appConfig.appUrl, appConfig.devMode),
        users = BillingUserDirectory { accounts.getAccountProfile.execute(it)?.email },
        clock = clock,
        tx = tx,
        http = http,
    )
    val wardrobe = WardrobeComponents(
        config = WardrobeConfig.from(config),
        imageStore = imageStore,
        textModel = gemini,
        taggerModel = geminiConfig.taggerModel,
        plans = PlanStatusAdapter(billing.walletSummary),
        clock = clock,
        http = http,
    )
    accounts = AccountsComponents(
        config = AccountsConfig.from(config, appConfig.appUrl),
        jwtSettings = jwtSettings,
        welcomeBonus = WelcomeBonusAdapter(billing.grantCredits, appConfig.signupBonus),
        balances = AccountBalanceAdapter(billing.walletSummary),
        avatars = AvatarUrlAdapter(wardrobe.getAvatar),
        limits = WardrobeLimitsAdapter(wardrobe.wardrobeLimits),
        erasers = listOf(
            AccountDataEraser { billing.closeBillingAccount.execute(it) },
            AccountDataEraser { wardrobe.eraseUserImages.execute(it) },
        ),
        clock = clock,
        tx = tx,
        scope = appScope,
    )
    val styling = StylingComponents(
        models = StylingModels(gemini, gemini, geminiConfig.stylistModel, geminiConfig.imageModel),
        imageStore = imageStore,
        wardrobe = WardrobeReaderAdapter(wardrobe.listClothes, wardrobe.getAvatar),
        wallet = CreditWalletAdapter(billing.reserveCredits, billing.confirmCredits, billing.refundCredits),
        locales = UserLocaleAdapter(accounts.getAccountProfile),
        renderedImages = RenderedImageSaverAdapter(wardrobe.uploadImage),
        clock = clock,
        tx = tx,
        scope = appScope,
    )

    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
                explicitNulls = true
            },
        )
    }
    install(CallLogging) {
        level = Level.INFO
        filter { !it.request.path().endsWith("/health") }
    }
    configureCors(appConfig)
    configureApiStatusPages()
    configureRateLimits(appConfig.rateLimits)
    install(Authentication) { configureJwtAuth(jwtSettings) }

    routing {
        healthRoute()
        route("/api/v1") {
            healthRoute()
            configureAccountsServiceRoutes(accounts)
            configureWardrobeServiceRoutes(wardrobe)
            configureStylingServiceRoutes(styling)
            configureBillingServiceRoutes(billing)
        }
    }

    styling.startBackgroundJobs()
    log.info("CloudFit started (env={}, appUrl={})", if (appConfig.devMode) "dev" else "prod", appConfig.appUrl)
}

private fun Route.healthRoute() {
    get("/health") { call.respond(HealthDto()) }
}

private fun Application.configureCors(appConfig: AppConfig) {
    install(CORS) {
        val appUrl = Url(appConfig.appUrl)
        allowHost(appUrl.hostWithPortIfSpecified(), schemes = listOf(appUrl.protocol.name))
        if (appConfig.devMode) allowHost("localhost:5173", schemes = listOf("http"))
        allowCredentials = true
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader("X-CloudFit-Client")
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
    }
}

private fun Url.hostWithPortIfSpecified(): String =
    if (specifiedPort == 0 || specifiedPort == protocol.defaultPort) host else "$host:$specifiedPort"
