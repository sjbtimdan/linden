import org.gradle.api.JavaVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.detekt)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(libs.compose.components.resources)
    detektPlugins(libs.detekt.ktlint.wrapper)
}

detekt {
    toolVersion = "2.0.0-alpha.6"
    config.setFrom("$rootDir/detekt.yml")
    buildUponDefaultConfig = true
    // Scope: formatting only (ktlint-wrapper ruleset from detektPlugins).
    disableDefaultRuleSets = true
}

// detekt is enforced by the pre-commit hook; keep it out of `check` for now.
tasks.named("check") {
    dependsOn.removeIf { it.toString().contains("detekt") }
}

compose.resources {
    packageOfResClass = "org.sjbtimdan.linden.generated.resources"
}

tasks.register<JavaExec>("renderIcon") {
    workingDir = projectDir.parentFile
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("org.sjbtimdan.linden.IconRendererKt")
}

compose.desktop {
    application {
        mainClass = "org.sjbtimdan.linden.MainKt"

        nativeDistributions {
            packageName = "org.sjbtimdan.linden"
            packageVersion = "1.0.0"
            description = "Linden expense tracker"
            vendor = "Linden"
            macOS {
                iconFile.set(project.file("icons/icon.icns"))
            }
            windows {
                iconFile.set(project.file("icons/icon.ico"))
            }
            linux {
                iconFile.set(project.file("icons/icon.png"))
            }
        }
    }
}
