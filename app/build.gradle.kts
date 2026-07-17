import com.android.build.api.dsl.ApplicationExtension

plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.benManes.versions)
	alias(libs.plugins.play.publisher)
	alias(libs.plugins.kotlin.compose.compiler)
	alias(libs.plugins.androidx.baselineprofile)
}

dependencies {

	implementation(libs.androidx.profileinstaller)
	implementation(libs.libsuperuser)
	implementation(libs.google.material)
	implementation(libs.gson)
	implementation(libs.jbcrypt)
	implementation(libs.guava)
	implementation(libs.annimon.stream)
	implementation(libs.volley)

	implementation(libs.zxing.embedded) {
		isTransitive = false
	}
	implementation(libs.zxing.core)

	implementation(libs.androidx.core.splashscreen)
	implementation(libs.androidx.constraintlayout)
	implementation(libs.androidx.documentfile)
	implementation(libs.androidx.lifecycle.runtime.ktx)
	implementation(libs.androidx.activity.compose)
	implementation(platform(libs.androidx.compose.bom))
	implementation(libs.androidx.compose.ui)
	implementation(libs.androidx.compose.ui.tooling)
	implementation(libs.androidx.compose.icons)
	implementation(libs.androidx.navigation.compose)
	implementation(libs.androidx.compose.material3)
	implementation(libs.androidx.preference)
	"baselineProfile"(project(":baselineprofile"))

	implementation(libs.androidx.work.runtime) // Java only
	implementation(libs.androidx.work.runtime.ktx) // Kotlin + coroutines
	implementation(libs.androidx.work.multiprocess) // Multiprocess support

	implementation(libs.compose.preference)
	implementation(libs.vico.compose)
}

/* For testing only
android {
    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}
*/



configure<ApplicationExtension> {
	// Changes to these values need to be reflected in `../docker/Dockerfile`
	//noinspection GradleDependency
	compileSdk = libs.versions.compileSdk.get().toInt()
	buildToolsVersion = libs.versions.buildTools.get()
	ndkVersion = libs.versions.ndk.get()

	buildFeatures {
		dataBinding = true
		viewBinding = true
		buildConfig = true
		compose = true
		resValues = true
	}

	defaultConfig {
		applicationId = "dev.benedek.syncthingandroid"
		minSdk = libs.versions.minSdk.get().toInt()
		targetSdk = libs.versions.targetSdk.get().toInt()
		versionCode = 4701
		versionName = "2.1.2.1"
		testApplicationId = "dev.benedek.syncthingandroid.test"
		manifestPlaceholders["appName"] = "@string/app_name"
		//testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
		resValue("string", "app_package_id", applicationId!!)
	}

	// For release-app.yaml
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

			manifestPlaceholders["appName"] = "Syncthing Debug"

			resValue(
				"string",
				"app_package_id",
				"${defaultConfig.applicationId}$applicationIdSuffix"
			)
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
		}
		create("beta") {
			initWith(getByName("release"))
			applicationIdSuffix = ".beta"

			manifestPlaceholders["appName"] = "Syncthing Beta"

			resValue(
				"string",
				"app_package_id",
				"${defaultConfig.applicationId}$applicationIdSuffix"
			)
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

base {
	archivesName.set("syncthing-android-${android.defaultConfig.versionName}")
}


tasks.named("preBuild") {
	dependsOn(":syncthing:buildNative")
}

