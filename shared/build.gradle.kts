import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.kover)
    alias(libs.plugins.detekt)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    jvm()

    android {
        namespace = "org.sjbtimdan.linden.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
            implementation(libs.sqldelight.android.driver)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.materialIconsCore)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotest.framework.engine)
            implementation(libs.kotest.assertions.core)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.compose.ui.test)
        }
        jvmMain.dependencies {
            implementation(libs.sqldelight.jdbc.driver)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.ktor.client.cio)
        }
        jvmTest.dependencies {
            implementation(libs.sqldelight.jdbc.driver)
            implementation(libs.kotest.runner.junit5)
            implementation(compose.desktop.currentOs)
        }

    }
}

sqldelight {
    databases {
        create("LindenDatabase") {
            packageName.set("org.sjbtimdan.linden.db")
            generateAsync.set(true)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
    detektPlugins(libs.detekt.ktlint.wrapper)
}

detekt {
    toolVersion = "2.0.0-alpha.6"
    config.setFrom("$rootDir/detekt.yml")
    buildUponDefaultConfig = true
    // Scope: formatting only (ktlint-wrapper ruleset from detektPlugins).
    disableDefaultRuleSets = true
    // The plain `detekt` task has no sources by default in KMP; wire up the
    // source-set dirs so `./gradlew detekt [--auto-correct]` covers everything.
    source.setFrom(
        "src/commonMain/kotlin",
        "src/commonTest/kotlin",
        "src/jvmMain/kotlin",
        "src/jvmTest/kotlin",
        "src/androidMain/kotlin",
    )
}

kover {
    reports {
        // Coverage is only meaningful for the JVM target: the Android target
        // only runs device tests, which Kover does not support.
        variant("jvm") {
            verify {
                rule {
                    minBound(50)
                }
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    // Pin the JVM locale so amount-formatting assertions are deterministic.
    systemProperty("user.language", "en")
    systemProperty("user.country", "US")
}

tasks.named("check") {
    dependsOn(tasks.named("koverVerifyJvm"))
    // detekt is enforced by the pre-commit hook; keep it out of `check` for now.
    dependsOn.removeIf { it.toString().contains("detekt") }
}