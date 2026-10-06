package app.cloudfit.shared.domain

import kotlinx.serialization.Serializable

@Serializable
enum class ClothingCategory { TOP, BOTTOM, DRESS, SHOES, OUTERWEAR, ACCESSORIES }

@Serializable
enum class Formality { CASUAL, SMART_CASUAL, FORMAL }

@Serializable
enum class Warmth { LIGHT, MEDIUM, WARM }
