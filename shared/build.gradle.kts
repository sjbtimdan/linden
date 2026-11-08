import java.io.File
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

// Compose compiler stability reports: metricsDestination emits per-class
// stability tables; reportsDestination emits the "skippable/unstable" report.
val composeMetricsDir = layout.buildDirectory.dir("compose_metrics")
val composeReportsDir = layout.buildDirectory.dir("compose_reports")

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        freeCompilerArgs.addAll(
            listOf(
                "-P",
                "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=${composeMetricsDir.get().asFile.absolutePath}",
                "-P",
                "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=${composeReportsDir.get().asFile.absolutePath}",
            ),
        )
    }

    jvm()

    android {
        namespace = "org.sjbtimdan.linden.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
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
            implementation(libs.haze)
            implementation(libs.haze.blur)
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

    jvm {
        val mainCompilation = compilations["main"]
        val testCompilation = compilations["test"]
        compilations.create("integrationTest") {
            // Associate with main + test so internal members (e.g.
            // DatabaseDriverFactory.createDriverAt) are visible and the test
            // dependencies (Kotest, JDBC driver) are on the classpath.
            associateWith(mainCompilation)
            associateWith(testCompilation)
        }
    }
    sourceSets {
        val jvmIntegrationTest = getByName("jvmIntegrationTest") {
            dependencies {
                // The integration test source set compiles only its own sources
                // but sees the compiled jvmTest classes and their dependencies
                // (helpers, Kotest, JDBC driver) on its classpath.
                implementation(files(this@kotlin.jvm().compilations["test"].output.allOutputs))
                implementation(files(this@kotlin.jvm().compilations["test"].compileDependencyFiles))
            }
        }
    }
}

kotlin.sourceSets.getByName("commonMain").kotlin.srcDir(
    layout.buildDirectory.dir("generated/buildInfo/kotlin"),
)

compose.resources {
    // Short, stable package for the generated Res class (default would embed
    // the module name: org.sjbtimdan.linden.shared.generated.resources).
    packageOfResClass = "org.sjbtimdan.linden.resources"
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

// `./gradlew fastCheck` and `-PnoKover` skip coverage instrumentation entirely;
// `check` keeps the Kover agent and the 50% gate.
val fastCheckRequested = gradle.startParameter.taskNames.any { it.substringAfterLast(':') == "fastCheck" }
val noKover = fastCheckRequested || providers.gradleProperty("noKover").isPresent

kover {
    if (noKover) disable()
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
    // Six forks is the sweet spot on this 8-core (6P+2E) dev machine: eight
    // test JVMs oversubscribe the performance cores and slow the suite. The cap
    // also avoids overforking CI runners with fewer cores.
    // Override with -PtestForks=N when benchmarking on another machine.
    val defaultForks = Math.min(6, Runtime.getRuntime().availableProcessors())
    maxParallelForks = providers.gradleProperty("testForks").orNull?.toIntOrNull() ?: defaultForks
    // Pin the JVM locale so amount-formatting assertions are deterministic.
    systemProperty("user.language", "en")
    systemProperty("user.country", "US")
    // -D flags reach the Gradle JVM, not forked test JVMs; forward the manual
    // prediction quality harness flag so the documented CLI command works.
    providers.systemProperty("descriptionQualityTest").orNull?.let {
        systemProperty("descriptionQualityTest", it)
    }
    providers.systemProperty("descriptionQualitySampleSize").orNull?.let {
        systemProperty("descriptionQualitySampleSize", it)
    }
    // Same forwarding for the manual Pace backtest harness.
    listOf(
        "paceBacktest",
        "paceBacktestAsOf",
        "paceBacktestDays",
        "paceBacktestZone",
        "paceBacktestOut",
    ).forEach { name ->
        providers.systemProperty(name).orNull?.let { systemProperty(name, it) }
    }
    // Cache test results so an unchanged suite is skipped (locally, via the
    // up-to-date check + build cache) or restored (remote cache). Only the
    // in-memory suites are deterministic enough to cache; integrationTest
    // mutates a real on-disk database, so it is excluded.
    if (name != "integrationTest") {
        outputs.cacheIf { true }
    }
}

// Integration tests live in their own source set (jvmIntegrationTest) so they
// are never part of the normal test run; run them manually with
// ./gradlew :shared:integrationTest.
val integrationCompilation = kotlin.jvm().compilations.getByName("integrationTest")
tasks.register<Test>("integrationTest") {
    description = "Runs integration tests against a real database (manual only)"
    group = "integration"
    testClassesDirs = files(integrationCompilation.output.allOutputs)
    classpath = integrationCompilation.runtimeDependencyFiles + files(integrationCompilation.output.allOutputs)
}

if (!noKover) {
    tasks.named("check") {
        dependsOn(tasks.named("koverVerifyJvm"))
    }
}

// Fast development check: runs jvmTest with Kover instrumentation disabled.
// Use ./gradlew fastCheck for rapid iteration, ./gradlew check for CI coverage.
tasks.register("fastCheck") {
    dependsOn("jvmTest")
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
    val gitTagged = providers.exec {
        commandLine("git", "describe", "--tags", "--exact-match")
        isIgnoreExitValue = true
    }.standardOutput.asText.get().trim().isNotEmpty()
    inputs.property("version", version)
    inputs.property("gitCommit", gitCommit)
    inputs.property("gitTagged", gitTagged)
    outputs.dir(outputDir)
    val repoRoot: File = rootDir
    doLast {
        // Read the dirty flag at execution time: its output changes on every
        // edit, so declaring it as a configuration-cache input would discard
        // the cached task graph on each build.
        val gitDirty = ProcessBuilder("git", "status", "--porcelain")
            .directory(repoRoot)
            .redirectErrorStream(true)
            .start()
            .inputStream.bufferedReader()
            .use { it.readText().isNotBlank() }
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
            |    const val GIT_TAGGED = $gitTagged
            |    const val BUILD_TIME = "$buildTime"
            |}
            |""".trimMargin(),
        )
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    dependsOn(generateBuildInfo)
}
