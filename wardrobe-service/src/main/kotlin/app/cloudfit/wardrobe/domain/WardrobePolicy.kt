package app.cloudfit.wardrobe.domain

data class WardrobePolicy(
    val freeMaxClothes: Int = 30,
    val maxUploadBytes: Long = 10L * 1024 * 1024,
)
