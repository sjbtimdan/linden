import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidTest)
    alias(libs.plugins.detekt)
}

android {
    namespace = "org.sjbtimdan.linden.benchmark"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Macrobenchmarks measure a non-debuggable variant of the app. The benchmark
    // module's `benchmark` build type matches `:androidApp`'s own `benchmark`
    // build type (a non-minified release-like variant signed with the debug key).
    // The test APK itself is also debug-signed for local `connectedCheck`.
    buildTypes {
        create("benchmark") {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    targetProjectPath = ":androidApp"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.runner)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.benchmark.macro.junit4)
    detektPlugins(libs.detekt.ktlint.wrapper)
}

detekt {
    toolVersion = "2.0.0-alpha.6"
    config.setFrom("$rootDir/detekt.yml")
    buildUponDefaultConfig = true
    disableDefaultRuleSets = true
}

// The `com.android.test` module only ever runs the `benchmark` variant; disable the
// default `debug` variant so `connectedCheck` targets the release-matching build type.
androidComponents {
    beforeVariants(selector().all()) {
        it.enable = it.buildType == "benchmark"
    }
}
