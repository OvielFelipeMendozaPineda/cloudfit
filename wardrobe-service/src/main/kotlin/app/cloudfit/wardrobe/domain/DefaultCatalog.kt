package app.cloudfit.wardrobe.domain

import app.cloudfit.shared.domain.ClothingCategory

data class DefaultClothe(
    val id: String,
    val category: ClothingCategory,
    val imageUrl: String,
    val name: String,
    val colour: String,
)

object DefaultCatalog {
    val clothes = listOf(
        DefaultClothe("default-top", ClothingCategory.TOP, "TODO-s3-default-top.png", "White tee", "white"),
        DefaultClothe("default-bottom", ClothingCategory.BOTTOM, "TODO-s3-default-bottom.png", "Blue jeans", "blue"),
        DefaultClothe("default-shoes", ClothingCategory.SHOES, "TODO-s3-default-shoes.png", "White sneakers", "white"),
    )

    val avatars = listOf(
        "TODO-s3-default-avatar-1.png",
        "TODO-s3-default-avatar-2.png",
    )
}
