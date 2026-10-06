package app.cloudfit.wardrobe.application.usecase

import app.cloudfit.shared.application.ai.AiCallContext
import app.cloudfit.shared.application.error.ErrorCodes
import app.cloudfit.shared.application.error.ProviderNotConfiguredException
import app.cloudfit.shared.application.error.UpstreamException
import app.cloudfit.shared.domain.ClothingCategory
import app.cloudfit.shared.testing.InMemoryImageStore
import app.cloudfit.wardrobe.domain.ClotheTags
import app.cloudfit.wardrobe.support.InMemoryAvatarRepository
import app.cloudfit.wardrobe.support.InMemoryUserImageRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import java.util.UUID
import kotlinx.coroutines.currentCoroutineContext

class ImageServicesTest : StringSpec({

    "uploads are stored and recorded as owned by the user" {
        val store = InMemoryImageStore()
        val images = InMemoryUserImageRepository()
        val user = UUID.randomUUID()

        val url = UploadImageService(store, images).execute(user, byteArrayOf(1, 2), "image/png")

        url shouldStartWith "/api/v1/images/"
        images.listKeys(user).single() shouldBe url.substringAfterLast('/')
        GetImageService(store).execute(url.substringAfterLast('/'))?.bytes?.toList() shouldBe listOf<Byte>(1, 2)
    }

    "erasing a user deletes only that user's images" {
        val store = InMemoryImageStore()
        val images = InMemoryUserImageRepository()
        val upload = UploadImageService(store, images)
        val ana = UUID.randomUUID()
        val bob = UUID.randomUUID()
        upload.execute(ana, byteArrayOf(1), "image/png")
        upload.execute(bob, byteArrayOf(2), "image/png")

        EraseUserImagesService(store, images).execute(ana)

        store.images.size shouldBe 1
        store.images.values.single().bytes.toList() shouldBe listOf<Byte>(2)
    }

    "remove-bg answers PROVIDER_NOT_CONFIGURED without an API key" {
        val upload = UploadImageService(InMemoryImageStore(), InMemoryUserImageRepository())
        shouldThrow<ProviderNotConfiguredException> {
            RemoveBackgroundService(null, upload).execute(UUID.randomUUID(), byteArrayOf(1), "image/jpeg")
        }
    }

    "remove-bg stores the PNG returned by the remover and maps failures to UPSTREAM_ERROR" {
        val store = InMemoryImageStore()
        val upload = UploadImageService(store, InMemoryUserImageRepository())

        val url = RemoveBackgroundService({ _, _ -> byteArrayOf(9) }, upload).execute(UUID.randomUUID(), byteArrayOf(1), "image/jpeg")
        url.endsWith(".png") shouldBe true

        shouldThrow<UpstreamException> {
            RemoveBackgroundService({ _, _ -> error("402") }, upload).execute(UUID.randomUUID(), byteArrayOf(1), "image/jpeg")
        }.code shouldBe ErrorCodes.UPSTREAM_ERROR
    }

    "tagging runs inside an AI call context and failures become AI_UNAVAILABLE" {
        val user = UUID.randomUUID()
        var seen: AiCallContext? = null
        val tags = ClotheTags("Tee", ClothingCategory.TOP, "white", "solid", null, null, null)

        TagClotheService { _, _ -> seen = currentCoroutineContext()[AiCallContext]; tags }.execute(user, byteArrayOf(1), "image/png") shouldBe tags
        seen?.userId shouldBe user
        seen?.operation shouldBe "TAG"

        shouldThrow<UpstreamException> {
            TagClotheService { _, _ -> error("boom") }.execute(user, byteArrayOf(1), "image/png")
        }.code shouldBe ErrorCodes.AI_UNAVAILABLE
    }

    "avatar can be set, read and deleted" {
        val repo = InMemoryAvatarRepository()
        val user = UUID.randomUUID()

        SetAvatarService(repo).execute(user, " /api/v1/images/a.png ") shouldBe "/api/v1/images/a.png"
        GetAvatarService(repo).execute(user) shouldBe "/api/v1/images/a.png"
        DeleteAvatarService(repo).execute(user)
        GetAvatarService(repo).execute(user) shouldBe null
    }
})
