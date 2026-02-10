plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.kover)
}

fun loadDotenv(file: java.io.File): Map<String, String> {
    if (!file.exists()) {
        return emptyMap()
    }

    return file
        .readLines()
        .asSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains("=") }
        .mapNotNull { line ->
            val index = line.indexOf("=")
            if (index <= 0) {
                return@mapNotNull null
            }

            val key = line.substring(0, index).trim()
            var value = line.substring(index + 1).trim()
            if ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith('\'') && value.endsWith('\''))) {
                value = value.substring(1, value.length - 1)
            }

            if (key.isEmpty()) {
                null
            } else {
                key to value
            }
        }.toMap()
}

val dotenv = loadDotenv(rootProject.projectDir.resolve(".env"))
val debugBaseUrl =
    dotenv["EGOGRAPH_BASE_URL_DEBUG"]?.takeIf { it.isNotBlank() }
        ?: project.findProperty("EGOGRAPH_BASE_URL_DEBUG") as? String
        ?: "http://10.0.2.2:8000"

val debugGatewayBaseUrl =
    dotenv["EGOGRAPH_GATEWAY_BASE_URL_DEBUG"]?.takeIf { it.isNotBlank() }
        ?: project.findProperty("EGOGRAPH_GATEWAY_BASE_URL_DEBUG") as? String
        ?: "http://10.0.2.2:8001"

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Compose Multiplatform
                implementation(compose.ui)
                implementation(compose.material3)
                implementation(compose.foundation)
                implementation(compose.materialIconsExtended)
                implementation(compose.components.resources)

                // Ktor Client
                implementation(libs.bundles.ktor.common)

                // Voyager Navigation
                implementation(libs.bundles.voyager)
                implementation(libs.voyager.koin)

                // MVIKotlin
                implementation(libs.bundles.mvikotlin)
                implementation(libs.mvikotlin.logging)

                // Koin DI
                implementation(libs.koin.core)
                implementation(libs.koin.compose)

                // Kermit Logging
                implementation(libs.kermit)

                // Markdown Renderer
                implementation(libs.markdown.renderer.m3)
                implementation(libs.markdown.renderer.code)

                // Kotlinx
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.appcompat)
                implementation(libs.ktor.client.android)
                implementation(libs.ktor.client.okhttp)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.turbine)
                implementation(libs.koin.test)
                implementation(libs.ktor.client.mock)
                implementation(libs.mockk)
            }
        }
    }
}

android {
    namespace = "dev.egograph.shared"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        buildConfigField(
            "String",
            "DEBUG_BASE_URL",
            "\"${debugBaseUrl}\"",
        )
        buildConfigField(
            "String",
            "STAGING_BASE_URL",
            "\"${project.findProperty("EGOGRAPH_BASE_URL_STAGING") ?: "http://192.168.0.2:8000"}\"",
        )
        buildConfigField(
            "String",
            "RELEASE_BASE_URL",
            "\"${project.findProperty("EGOGRAPH_BASE_URL_RELEASE") ?: "https://api.egograph.dev"}\"",
        )
        buildConfigField(
            "String",
            "DEBUG_GATEWAY_BASE_URL",
            "\"${debugGatewayBaseUrl}\"",
        )
        buildConfigField(
            "String",
            "STAGING_GATEWAY_BASE_URL",
            "\"${project.findProperty("EGOGRAPH_GATEWAY_BASE_URL_STAGING") ?: "http://192.168.0.2:8001"}\"",
        )
        buildConfigField(
            "String",
            "RELEASE_GATEWAY_BASE_URL",
            "\"${project.findProperty("EGOGRAPH_GATEWAY_BASE_URL_RELEASE") ?: "https://gateway.egograph.dev"}\"",
        )
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
    }

    sourceSets {
        getByName("main").assets.srcDir("src/commonMain/resources/assets")
    }

    lint {
        baseline = file("lint-baseline.xml")
    }
}

compose.resources {
    publicResClass = true
}

kover {
    reports {
        total {
            html {
                onCheck = false
            }
            xml {
                onCheck = false
            }
        }
    }
    currentProject {
        instrumentation {
            excludedClasses.add("*.ui.*")
            excludedClasses.add("*Screen")
            excludedClasses.add("*Screen\$*")
        }
    }
}
