import java.time.Instant
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
            implementation(libs.compose.materialIconsExtended)
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

kotlin.sourceSets.getByName("commonMain").kotlin.srcDir(
    layout.buildDirectory.dir("generated/buildInfo/kotlin"),
)

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
    // Run test classes in parallel across forked JVMs: each fork is isolated, so
    // Compose UI tests keep running sequentially per fork while unrelated
    // test classes (DAOs, parsers, ViewModels) execute concurrently.
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceIn(1, 4)
    // Pin the JVM locale so amount-formatting assertions are deterministic.
    systemProperty("user.language", "en")
    systemProperty("user.country", "US")
}

tasks.named("check") {
    dependsOn(tasks.named("koverVerifyJvm"))
}

// Generates a BuildInfo.kt file in commonMain with the app version and the
// current git commit, so the running build can be identified from the UI.
// The version is derived from git tags: on a tag it's the tag version, otherwise
// it's the last tag plus the number of commits since it (e.g. "0.1.1-3"). Until
// the first tag exists it falls back to the base version plus the total commit
// count (e.g. "0.1.0-42").
val generateBuildInfo = tasks.register("generateBuildInfo") {
    val outputDir = layout.buildDirectory.dir("generated/buildInfo/kotlin")
    val baseVersion = providers.gradleProperty("lindenVersion").getOrElse("0.0.0")
    val gitDescribe = providers.exec {
        commandLine("git", "describe", "--tags", "--always")
        isIgnoreExitValue = true
    }.standardOutput.asText.get().trim()
    val version = when {
        // No tags yet: describe returns a bare commit hash -> base + commit count.
        gitDescribe.matches(Regex("[0-9a-f]{7,40}")) -> {
            val count = providers.exec {
                commandLine("git", "rev-list", "--count", "HEAD")
                isIgnoreExitValue = true
            }.standardOutput.asText.get().trim()
            "$baseVersion-$count"
        }
        else -> gitDescribe
            .removePrefix("v")
            // Drop the "-g<sha>" suffix: the commit is shown separately.
            .replace(Regex("-g[0-9a-f]{7,40}$"), "")
    }
    val gitCommit = providers.exec {
        commandLine("git", "rev-parse", "--short", "HEAD")
        isIgnoreExitValue = true
    }.standardOutput.asText.get().trim().ifEmpty { "unknown" }
    val gitDirty = providers.exec {
        commandLine("git", "status", "--porcelain")
        isIgnoreExitValue = true
    }.standardOutput.asText.get().isNotBlank()
    inputs.property("version", version)
    inputs.property("gitCommit", gitCommit)
    inputs.property("gitDirty", gitDirty)
    outputs.dir(outputDir)
    doLast {
        val buildTime = Instant.now().toString()
        val file = outputDir.get().file("org/sjbtimdan/linden/BuildInfo.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            |package org.sjbtimdan.linden
            |
            |/** Build-time metadata recorded at compile time. */
            |object BuildInfo {
            |    const val VERSION = "$version"
            |    const val GIT_COMMIT = "$gitCommit"
            |    const val GIT_DIRTY = $gitDirty
            |    const val BUILD_TIME = "$buildTime"
            |}
            |""".trimMargin(),
        )
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    dependsOn(generateBuildInfo)
}
