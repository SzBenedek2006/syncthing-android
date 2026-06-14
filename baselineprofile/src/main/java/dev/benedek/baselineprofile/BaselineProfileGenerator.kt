package dev.benedek.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.regex.Pattern

/**
 * This test class generates a basic startup baseline profile for the target package.
 *
 * We recommend you start with this but add important user flows to the profile to improve their performance.
 * Refer to the [baseline profile documentation](https://d.android.com/topic/performance/baselineprofiles)
 * for more information.
 *
 * You can run the generator with the "Generate Baseline Profile" run configuration in Android Studio or
 * the equivalent `generateBaselineProfile` gradle task:
 * ```
 * ./gradlew :app:generateReleaseBaselineProfile
 * ```
 * The run configuration runs the Gradle task and applies filtering to run only the generators.
 *
 * Check [documentation](https://d.android.com/topic/performance/benchmarking/macrobenchmark-instrumentation-args)
 * for more information about available instrumentation arguments.
 *
 * After you run the generator, you can verify the improvements running the [StartupBenchmarks] benchmark.
 *
 * When using this class to generate a baseline profile, only API 33+ or rooted API 28+ are supported.
 *
 * The minimum required version of androidx.benchmark to generate a baseline profile is 1.2.0.
 **/
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

	@get:Rule
	val rule = BaselineProfileRule()

	@Test
	fun generate() {
		// The application id for the running build variant is read from the instrumentation arguments.

		// Minimal
//        rule.collect(
//            packageName = InstrumentationRegistry.getArguments().getString("targetAppId")
//                ?: throw Exception("targetAppId not passed as instrumentation runner arg"),
//
//            // See: https://d.android.com/topic/performance/baselineprofiles/dex-layout-optimizations
//            includeInStartupProfile = true
//        ) {
//            // Start default activity for your app
//            pressHome()
//            startActivityAndWait()
//        }


		rule.collect(
			packageName = InstrumentationRegistry.getArguments().getString("targetAppId")
				?: throw Exception("targetAppId not passed as instrumentation runner arg"),

			// See: https://d.android.com/topic/performance/baselineprofiles/dex-layout-optimizations
			includeInStartupProfile = false
		) {
			// This block defines the app's critical user journey. Here we are interested in
			// optimizing for app startup. But you can also navigate and scroll through your most important UI.

			// PERMISSIONS
			// Notifications (Android 13+)
			device.executeShellCommand("pm grant $packageName android.permission.POST_NOTIFICATIONS")

			// Location
			device.executeShellCommand("pm grant $packageName android.permission.ACCESS_COARSE_LOCATION")
			device.executeShellCommand("pm grant $packageName android.permission.ACCESS_FINE_LOCATION")

			// Location
			device.executeShellCommand("pm grant $packageName android.permission.ACCESS_BACKGROUND_LOCATION")

			// Standard Storage
			device.executeShellCommand("pm grant $packageName android.permission.READ_EXTERNAL_STORAGE")
			device.executeShellCommand("pm grant $packageName android.permission.WRITE_EXTERNAL_STORAGE")

			// All Files Access (Android 11+)
			device.executeShellCommand("appops set $packageName MANAGE_EXTERNAL_STORAGE allow")

			// Disable battery optimizations
			device.executeShellCommand("dumpsys deviceidle whitelist +$packageName")

			// Start default activity for your app
			pressHome()
			startActivityAndWait()


			val continueButton = device.findObject(By.text(Pattern.compile("Continue|Finish")))
			if (continueButton != null) {
				continueButton.click()
				device.waitForIdle()
			}

			val menuButton = device.findObject(By.desc("Menu"))
			if (menuButton != null) {
				menuButton.click()
				device.waitForIdle()
			}

			val settingsButton = device.findObject(By.desc("Settings"))
			if (settingsButton != null) {
				settingsButton.click()
				device.waitForIdle()
			}

			val themeButton = device.findObject(By.text("Theme"))
			if (themeButton != null) {
				themeButton.click()
				device.waitForIdle()
			}

			// Check UiAutomator documentation for more information how to interact with the app.
			// https://d.android.com/training/testing/other-components/ui-automator
		}

	}
}