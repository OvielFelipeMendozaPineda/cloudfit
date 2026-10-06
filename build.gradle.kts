plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kover)
    application
}

group = "app.cloudfit"
version = "0.1.0"

application {
    mainClass = "app.cloudfit.ApplicationKt"
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":shared-code"))
    implementation(project(":accounts-service"))
    implementation(project(":wardrobe-service"))
    implementation(project(":styling-service"))
    implementation(project(":billing-service"))

    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.cors)
    implementation(libs.logback.classic)

    testImplementation(testFixtures(project(":shared-code")))
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.testcontainers.postgresql)

    kover(project(":shared-code"))
    kover(project(":accounts-service"))
    kover(project(":wardrobe-service"))
    kover(project(":styling-service"))
    kover(project(":billing-service"))
}

tasks.test {
    useJUnitPlatform()
    if (System.getenv("TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE") == null) {
        environment("TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE", "/var/run/docker.sock")
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
    apply(plugin = "org.jetbrains.kotlinx.kover")
    apply(plugin = "java-library")

    group = "app.cloudfit"
    version = "0.1.0"

    extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
        jvmToolchain(21)
    }

    dependencies {
        if (name != "shared-code") {
            "implementation"(project(":shared-code"))
            "testImplementation"(testFixtures(project(":shared-code")))
        }
        "testImplementation"(rootProject.libs.kotest.runner.junit5)
        "testImplementation"(rootProject.libs.kotest.assertions.core)
        "testImplementation"(rootProject.libs.ktor.server.test.host)
        "testImplementation"(rootProject.libs.ktor.client.content.negotiation)
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    if (name != "shared-code") {
        extensions.configure<kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension> {
            reports {
                filters {
                    includes {
                        classes("app.cloudfit.*.application.usecase.*")
                    }
                }
                verify {
                    rule("usecase line coverage") {
                        minBound(80)
                    }
                }
            }
        }
    }
}

tasks.named("check") {
    dependsOn(subprojects.map { "${it.path}:koverVerify" })
}
