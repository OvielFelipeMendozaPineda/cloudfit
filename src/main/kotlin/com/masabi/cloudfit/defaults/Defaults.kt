package com.masabi.cloudfit.defaults

import com.masabi.cloudfit.avatar.Avatar
import com.masabi.cloudfit.clothes.Clothe
import com.masabi.cloudfit.clothes.ClothingCategory

// TODO paso 3: reemplazar estas imageUrl por las reales una vez subidas al bucket S3.
object Defaults {
    val clothes = listOf(
        Clothe(id = "default-top", category = ClothingCategory.TOP, imageUrl = "TODO-s3-default-top.png", name = "White tee", colour = "white"),
        Clothe(id = "default-bottom", category = ClothingCategory.BOTTOM, imageUrl = "TODO-s3-default-bottom.png", name = "Blue jeans", colour = "blue"),
        Clothe(id = "default-shoes", category = ClothingCategory.SHOES, imageUrl = "TODO-s3-default-shoes.png", name = "White sneakers", colour = "white"),
    )

    val avatars = listOf(
        Avatar(photoUrl = "TODO-s3-default-avatar-1.png"),
        Avatar(photoUrl = "TODO-s3-default-avatar-2.png"),
    )
}
