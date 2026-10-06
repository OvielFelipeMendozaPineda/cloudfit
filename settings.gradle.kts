pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "cloud-fit"

include(
    "shared-code",
    "accounts-service",
    "wardrobe-service",
    "styling-service",
    "billing-service",
)
