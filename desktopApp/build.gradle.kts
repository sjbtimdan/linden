import org.gradle.api.JavaVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
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
