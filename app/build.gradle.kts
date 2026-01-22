plugins {
    id("com.android.application")
    id("com.github.ben-manes.versions")
    id("com.github.triplet.play") version "3.13.0"
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
    implementation("commons-io:commons-io:2.21.0")

    implementation("com.journeyapps:zxing-android-embedded:4.3.0") {
        isTransitive = false
    }
    implementation("com.google.zxing:core:3.5.4")

    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("com.google.dagger:dagger:2.57.2")
    implementation("androidx.documentfile:documentfile:1.1.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.activity:activity-compose:1.12.2")
    implementation(platform("androidx.compose:compose-bom:2026.01.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.compose.ui:ui-graphics:1.10.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.01.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    annotationProcessor("com.google.dagger:dagger-compiler:2.59")
    androidTestImplementation("androidx.test:rules:1.7.0")
    androidTestImplementation("androidx.annotation:annotation:1.9.1")
    implementation("androidx.preference:preference:1.2.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    val workVersion = "2.11.0"
    implementation("androidx.work:work-runtime:${workVersion}") // (Java only)
    implementation("androidx.work:work-runtime-ktx:${workVersion}") // Kotlin + coroutines
    implementation("androidx.work:work-multiprocess:${workVersion}") // optional - Multiprocess support

    implementation("me.zhanghai.compose.preference:preference:2.1.1")
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
        versionCode = 4410
        versionName = "2.0.10.10"
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
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }


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


androidComponents {
    onVariants { variant ->

        variant.outputs.forEach { output ->
            // This abomination is needed for a simple renaming of an output file?
            (output as? com.android.build.api.variant.impl.VariantOutputImpl)?.outputFileName?.set(output.versionName.map { versionName ->
                "syncthing-android-${variant.name}_${versionName}.apk"
            })
        }

        // Tasks don't exist yet when onVariants runs, so must wait
        project.afterEvaluate {
            val taskName = "merge${variant.name.replaceFirstChar { it.titlecase() }}JniLibFolders"
            project.tasks.findByName(taskName)?.dependsOn(":syncthing:buildNative")
        }
    }
}