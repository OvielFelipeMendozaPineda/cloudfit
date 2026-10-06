package app.cloudfit.styling.infrastructure.config

import app.cloudfit.shared.application.ai.ImageModel
import app.cloudfit.shared.application.ai.TextModel
import app.cloudfit.shared.application.port.ClockProvider
import app.cloudfit.shared.application.port.ImageStore
import app.cloudfit.shared.application.port.TransactionRunner
import app.cloudfit.styling.application.port.output.CreditWallet
import app.cloudfit.styling.application.port.output.RenderedImageSaver
import app.cloudfit.styling.application.port.output.UserLocaleReader
import app.cloudfit.styling.application.port.output.WardrobeReader
import app.cloudfit.styling.application.usecase.CreateLookService
import app.cloudfit.styling.application.usecase.DeleteLookService
import app.cloudfit.styling.application.usecase.GetLookService
import app.cloudfit.styling.application.usecase.ListLooksService
import app.cloudfit.styling.application.usecase.ProcessLookService
import app.cloudfit.styling.application.usecase.RecoverStuckLooksService
import app.cloudfit.styling.application.usecase.SaveLookService
import app.cloudfit.styling.domain.StylingPolicy
import app.cloudfit.styling.infrastructure.adapter.input.http.registerLookRoutes
import app.cloudfit.styling.infrastructure.adapter.output.ai.LlmClothePicker
import app.cloudfit.styling.infrastructure.adapter.output.ai.LlmOutfitImageRenderer
import app.cloudfit.styling.infrastructure.adapter.output.jobs.CoroutineLookJobScheduler
import app.cloudfit.styling.infrastructure.adapter.output.jobs.StuckLooksSweeper
import app.cloudfit.styling.infrastructure.adapter.output.persistence.PostgresLookRepository
import io.ktor.server.routing.Route
import kotlinx.coroutines.CoroutineScope

data class StylingModels(
    val textModel: TextModel,
    val imageModel: ImageModel,
    val stylistModel: String,
    val imageModelName: String,
)

class StylingComponents(
    models: StylingModels,
    imageStore: ImageStore,
    wardrobe: WardrobeReader,
    wallet: CreditWallet,
    locales: UserLocaleReader,
    renderedImages: RenderedImageSaver,
    clock: ClockProvider,
    tx: TransactionRunner,
    scope: CoroutineScope,
    policy: StylingPolicy = StylingPolicy(),
) {
    private val looks = PostgresLookRepository(clock)
    private val processLook = ProcessLookService(
        looks = looks,
        wardrobe = wardrobe,
        locales = locales,
        picker = LlmClothePicker(models.textModel, models.stylistModel),
        renderer = LlmOutfitImageRenderer(models.imageModel, imageStore, models.imageModelName),
        images = renderedImages,
        wallet = wallet,
    )
    private val scheduler = CoroutineLookJobScheduler(scope, processLook)
    private val sweeper = StuckLooksSweeper(scope, RecoverStuckLooksService(looks, wallet, policy, clock))

    private val createLook = CreateLookService(looks, wardrobe, wallet, scheduler, policy, tx, clock)
    private val getLook = GetLookService(looks)
    private val listLooks = ListLooksService(looks, policy)
    private val saveLook = SaveLookService(looks)
    private val deleteLook = DeleteLookService(looks, wallet)

    fun startBackgroundJobs() {
        sweeper.start()
    }

    fun registerRoutes(route: Route) {
        route.registerLookRoutes(createLook, getLook, listLooks, saveLook, deleteLook)
    }
}

fun Route.configureStylingServiceRoutes(components: StylingComponents) {
    components.registerRoutes(this)
}
