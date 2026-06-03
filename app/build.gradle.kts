import com.android.build.api.dsl.ApplicationExtension

plugins {
    id("com.android.application")
    id("com.github.ben-manes.versions")
    id("com.github.triplet.play") version "4.0.0"
    id("org.jetbrains.kotlin.plugin.compose")
    id("androidx.baselineprofile")
}

dependencies {

    implementation("androidx.profileinstaller:profileinstaller:1.4.1")
    implementation("eu.chainfire:libsuperuser:1.1.1")
    implementation("com.google.android.material:material:1.14.0")
    implementation("com.google.code.gson:gson:2.14.0")
    implementation("org.mindrot:jbcrypt:0.4")
    implementation("com.google.guava:guava:33.6.0-android")
    implementation("com.annimon:stream:1.2.2")
    implementation("com.android.volley:volley:1.2.1")

    implementation("com.journeyapps:zxing-android-embedded:4.3.0") {
        isTransitive = false
    }
    implementation("com.google.zxing:core:3.5.4")

    implementation("androidx.core:core-splashscreen:1.2.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.documentfile:documentfile:1.1.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation(platform("androidx.compose:compose-bom:2026.05.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.navigation:navigation-compose:2.9.8")
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.preference:preference:1.2.1")
    "baselineProfile"(project(":baselineprofile"))

    val workVersion = "2.11.2"
    implementation("androidx.work:work-runtime:${workVersion}") // Java only
    implementation("androidx.work:work-runtime-ktx:${workVersion}") // Kotlin + coroutines
    implementation("androidx.work:work-multiprocess:${workVersion}") // Multiprocess support

    implementation("me.zhanghai.compose.preference:preference:2.2.0")
    val vicoVersion = "3.1.0"
    implementation("com.patrykandpatrick.vico:compose-m3:$vicoVersion")
}

configure<ApplicationExtension> {
    // Changes to these values need to be reflected in `../docker/Dockerfile`
    //noinspection GradleDependency
    compileSdk = 36
    buildToolsVersion = "36.0.0"
    ndkVersion = "29.0.14206865"

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
        versionCode = 4506
        versionName = "2.0.15.6"
        testApplicationId = "dev.benedek.syncthingandroid.test"
        //testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.runCatching { getByName("release") }
                .getOrNull()
                .takeIf { it?.storeFile != null }
            resValue("string", "app_package_id", "${defaultConfig.applicationId}")
        }
        create("beta") {
            initWith(getByName("release"))
            applicationIdSuffix = ".beta"
            resValue("string", "app_package_id", "${defaultConfig.applicationId}$applicationIdSuffix")
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

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }


}

play {
    serviceAccountCredentials.set(
        file(System.getenv("SYNCTHING_RELEASE_PLAY_ACCOUNT_CONFIG_FILE") ?: "keys.json")
    )
    track.set("beta")
}

androidComponents {
    onVariants { variant ->
        val ndkDirProvider = sdkComponents.ndkDirectory.map { it.asFile.absolutePath }
        gradle.rootProject.extra["ndk.dir"] = ndkDirProvider.get()

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