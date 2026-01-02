import com.android.build.api.dsl.Packaging
import org.gradle.configurationcache.extensions.capitalized

plugins {
    id("com.android.application")
    id("com.github.ben-manes.versions")
    id("com.github.triplet.play") version "3.12.1"
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

dependencies {

    implementation("eu.chainfire:libsuperuser:1.1.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("com.google.code.gson:gson:2.13.2")
    implementation("org.mindrot:jbcrypt:0.4")
    implementation("com.google.guava:guava:33.5.0-android")
    implementation("com.annimon:stream:1.2.2")
    implementation("com.android.volley:volley:1.2.1")
    implementation("commons-io:commons-io:2.20.0")

    implementation("com.journeyapps:zxing-android-embedded:4.3.0") {
        isTransitive = false
    }
    implementation("com.google.zxing:core:3.5.3")

    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("com.google.dagger:dagger:2.57.2")
    implementation("androidx.documentfile:documentfile:1.1.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.activity:activity-compose:1.12.0")
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-graphics:1.10.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    annotationProcessor("com.google.dagger:dagger-compiler:2.57.2")
    androidTestImplementation("androidx.test:rules:1.7.0")
    androidTestImplementation("androidx.annotation:annotation:1.9.1")
    implementation("androidx.preference:preference:1.2.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    val workVersion = "2.11.0"
    implementation("androidx.work:work-runtime:${workVersion}") // (Java only)
    implementation("androidx.work:work-runtime-ktx:${workVersion}") // Kotlin + coroutines
    implementation("androidx.work:work-multiprocess:${workVersion}") // optional - Multiprocess support
}

android {
    //val ndkVersionShared = rootProject.extra.get("ndkVersionShared") // not needed if built in docker
    // Changes to these values need to be reflected in `../docker/Dockerfile`
    //noinspection GradleDependency
    compileSdk = 36
    buildToolsVersion = "36.0.0"
    //ndkVersion = "$ndkVersionShared" // not needed if built in docker

    buildFeatures {
        dataBinding = true
        viewBinding = true
        buildConfig = true
        compose = true
    }

    defaultConfig {
        applicationId = "dev.benedek.syncthingandroid"
        minSdk = 23
        targetSdk = 36
        versionCode = 4408
        versionName = "2.0.10.8"
        testApplicationId = "dev.benedek.syncthingandroid.test"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = System.getenv("SYNCTHING_RELEASE_STORE_FILE")?.let(::file)
            storePassword = System.getenv("SIGNING_PASSWORD")
            keyAlias = System.getenv("SYNCTHING_RELEASE_KEY_ALIAS")
            keyPassword = System.getenv("SIGNING_PASSWORD")
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            isDebuggable = true
            isJniDebuggable = true
            isMinifyEnabled = false

            resValue("string", "app_package_id", "${defaultConfig.applicationId}$applicationIdSuffix")
        }
        getByName("release") {
            signingConfig = signingConfigs.runCatching { getByName("release") }
                .getOrNull()
                .takeIf { it?.storeFile != null }
            resValue("string", "app_package_id", "${defaultConfig.applicationId}")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    namespace = "dev.benedek.syncthingandroid"
}

play {
    serviceAccountCredentials.set(
        file(System.getenv("SYNCTHING_RELEASE_PLAY_ACCOUNT_CONFIG_FILE") ?: "keys.json")
    )
    track.set("beta")
}

/**
 * Some languages are not supported by Google Play, so we ignore them.
 */
tasks.register<Delete>("deleteUnsupportedPlayTranslations") {
    delete(
        "src/main/play/listings/de_DE/",
        "src/main/play/listings/el-EL/",
        "src/main/play/listings/en/",
        "src/main/play/listings/eo/",
        "src/main/play/listings/eu/",
        "src/main/play/listings/nb/",
        "src/main/play/listings/nl_BE/",
        "src/main/play/listings/nn/",
        "src/main/play/listings/ta/",
    )
}

project.afterEvaluate {
    android.buildTypes.forEach {
        tasks.named("merge${it.name.capitalized()}JniLibFolders") {
            dependsOn(":syncthing:buildNative")
        }
    }
}
