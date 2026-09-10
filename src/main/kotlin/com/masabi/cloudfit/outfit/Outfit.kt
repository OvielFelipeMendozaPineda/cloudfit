package com.masabi.cloudfit.outfit

import kotlinx.serialization.Serializable

@Serializable
enum class ImageStatus { PENDING, READY, FAILED }

@Serializable
data class Outfit(
    val id: String,
    val event: String? = null,
    val clotheIds: List<String>,
    val stylistNote: String? = null,
    val imageStatus: ImageStatus = ImageStatus.PENDING,
    val imageUrl: String? = null,
)
