package app.cloudfit.wardrobe.infrastructure.config

import app.cloudfit.shared.application.ai.TextModel
import app.cloudfit.shared.application.port.ClockProvider
import app.cloudfit.shared.application.port.ImageStore
import app.cloudfit.shared.infrastructure.auth.JWT_AUTH
import app.cloudfit.wardrobe.application.port.output.PlanStatusReader
import app.cloudfit.wardrobe.application.usecase.CreateClotheService
import app.cloudfit.wardrobe.application.usecase.DeleteAvatarService
import app.cloudfit.wardrobe.application.usecase.DeleteClotheService
import app.cloudfit.wardrobe.application.usecase.EraseUserImagesService
import app.cloudfit.wardrobe.application.usecase.GetAvatarService
import app.cloudfit.wardrobe.application.usecase.GetImageService
import app.cloudfit.wardrobe.application.usecase.GetWardrobeLimitsService
import app.cloudfit.wardrobe.application.usecase.ListClothesService
import app.cloudfit.wardrobe.application.usecase.RemoveBackgroundService
import app.cloudfit.wardrobe.application.usecase.SetAvatarService
import app.cloudfit.wardrobe.application.usecase.TagClotheService
import app.cloudfit.wardrobe.application.usecase.UpdateClotheService
import app.cloudfit.wardrobe.application.usecase.UploadImageService
import app.cloudfit.wardrobe.infrastructure.adapter.input.http.registerAvatarRoutes
import app.cloudfit.wardrobe.infrastructure.adapter.input.http.registerClothesRoutes
import app.cloudfit.wardrobe.infrastructure.adapter.input.http.registerDefaultsRoutes
import app.cloudfit.wardrobe.infrastructure.adapter.input.http.registerImageRoutes
import app.cloudfit.wardrobe.infrastructure.adapter.input.http.registerPublicImageRoutes
import app.cloudfit.wardrobe.infrastructure.adapter.output.ai.LlmClotheTagger
import app.cloudfit.wardrobe.infrastructure.adapter.output.persistence.PostgresAvatarRepository
import app.cloudfit.wardrobe.infrastructure.adapter.output.persistence.PostgresClothesRepository
import app.cloudfit.wardrobe.infrastructure.adapter.output.persistence.PostgresUserImageRepository
import app.cloudfit.wardrobe.infrastructure.adapter.output.removebg.RemoveBgBackgroundRemover
import io.ktor.client.HttpClient
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route

class WardrobeComponents(
    private val config: WardrobeConfig,
    imageStore: ImageStore,
    textModel: TextModel,
    taggerModel: String,
    plans: PlanStatusReader,
    clock: ClockProvider,
    http: HttpClient,
) {
    private val clothes = PostgresClothesRepository(clock)
    private val avatars = PostgresAvatarRepository(clock)
    private val images = PostgresUserImageRepository(clock)
    private val remover = config.removeBgApiKey.takeIf { it.isNotBlank() }?.let { RemoveBgBackgroundRemover(it, http) }

    val listClothes = ListClothesService(clothes)
    val wardrobeLimits = GetWardrobeLimitsService(plans, config.policy)
    val getAvatar = GetAvatarService(avatars)
    val uploadImage = UploadImageService(imageStore, images)
    val eraseUserImages = EraseUserImagesService(imageStore, images)

    private val createClothe = CreateClotheService(clothes, wardrobeLimits)
    private val updateClothe = UpdateClotheService(clothes)
    private val deleteClothe = DeleteClotheService(clothes)
    private val removeBackground = RemoveBackgroundService(remover, uploadImage)
    private val tagClothe = TagClotheService(LlmClotheTagger(textModel, taggerModel))
    private val getImage = GetImageService(imageStore)
    private val setAvatar = SetAvatarService(avatars)
    private val deleteAvatar = DeleteAvatarService(avatars)

    fun registerRoutes(route: Route) {
        route.registerDefaultsRoutes()
        route.registerPublicImageRoutes(getImage)
        route.authenticate(JWT_AUTH) {
            registerClothesRoutes(listClothes, createClothe, updateClothe, deleteClothe)
            registerImageRoutes(uploadImage, removeBackground, tagClothe, config.policy.maxUploadBytes)
            registerAvatarRoutes(getAvatar, setAvatar, deleteAvatar)
        }
    }
}

fun Route.configureWardrobeServiceRoutes(components: WardrobeComponents) {
    components.registerRoutes(this)
}
