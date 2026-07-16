plugins {
	id("com.android.test")
	id("androidx.baselineprofile")
}

android {
	namespace = "dev.benedek.baselineprofile"
	compileSdk = 36

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_21
		targetCompatibility = JavaVersion.VERSION_21
	}

	defaultConfig {
		minSdk = 28
		targetSdk = 36

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
	}

	targetProjectPath = ":app"

}

// This is the configuration block for the Baseline Profile plugin.
// You can specify to run the generators on a managed devices or connected devices.
baselineProfile {
	useConnectedDevices = true
}

dependencies {
	implementation(libs.androidx.benchmark.macro)
	implementation(libs.androidx.test.espresso.core)
	implementation(libs.androidx.test.ext.junit)
	implementation(libs.androidx.test.uiautomator)
}


androidComponents {
	onVariants { v ->
		val artifactsLoader = v.artifacts.getBuiltArtifactsLoader()
		v.instrumentationRunnerArguments.put(
			"targetAppId",
			v.testedApks.map { artifactsLoader.load(it)?.applicationId }
		)
	}
}
