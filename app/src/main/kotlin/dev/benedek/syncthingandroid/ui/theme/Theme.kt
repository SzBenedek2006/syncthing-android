package dev.benedek.syncthingandroid.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import dev.benedek.syncthingandroid.util.ThemeControls


@Immutable
data class ExtendedColorScheme(
	val blue: ColorFamily,
	val red: ColorFamily,
	val green: ColorFamily,
	val yellow: ColorFamily
)

private val lightScheme = lightColorScheme(
	primary = primaryLight,
	onPrimary = onPrimaryLight,
	primaryContainer = primaryContainerLight,
	onPrimaryContainer = onPrimaryContainerLight,
	secondary = secondaryLight,
	onSecondary = onSecondaryLight,
	secondaryContainer = secondaryContainerLight,
	onSecondaryContainer = onSecondaryContainerLight,
	tertiary = tertiaryLight,
	onTertiary = onTertiaryLight,
	tertiaryContainer = tertiaryContainerLight,
	onTertiaryContainer = onTertiaryContainerLight,
	error = errorLight,
	onError = onErrorLight,
	errorContainer = errorContainerLight,
	onErrorContainer = onErrorContainerLight,
	background = backgroundLight,
	onBackground = onBackgroundLight,
	surface = surfaceLight,
	onSurface = onSurfaceLight,
	surfaceVariant = surfaceVariantLight,
	onSurfaceVariant = onSurfaceVariantLight,
	outline = outlineLight,
	outlineVariant = outlineVariantLight,
	scrim = scrimLight,
	inverseSurface = inverseSurfaceLight,
	inverseOnSurface = inverseOnSurfaceLight,
	inversePrimary = inversePrimaryLight,
	surfaceDim = surfaceDimLight,
	surfaceBright = surfaceBrightLight,
	surfaceContainerLowest = surfaceContainerLowestLight,
	surfaceContainerLow = surfaceContainerLowLight,
	surfaceContainer = surfaceContainerLight,
	surfaceContainerHigh = surfaceContainerHighLight,
	surfaceContainerHighest = surfaceContainerHighestLight,
)

private val darkScheme = darkColorScheme(
	primary = primaryDark,
	onPrimary = onPrimaryDark,
	primaryContainer = primaryContainerDark,
	onPrimaryContainer = onPrimaryContainerDark,
	secondary = secondaryDark,
	onSecondary = onSecondaryDark,
	secondaryContainer = secondaryContainerDark,
	onSecondaryContainer = onSecondaryContainerDark,
	tertiary = tertiaryDark,
	onTertiary = onTertiaryDark,
	tertiaryContainer = tertiaryContainerDark,
	onTertiaryContainer = onTertiaryContainerDark,
	error = errorDark,
	onError = onErrorDark,
	errorContainer = errorContainerDark,
	onErrorContainer = onErrorContainerDark,
	background = backgroundDark,
	onBackground = onBackgroundDark,
	surface = surfaceDark,
	onSurface = onSurfaceDark,
	surfaceVariant = surfaceVariantDark,
	onSurfaceVariant = onSurfaceVariantDark,
	outline = outlineDark,
	outlineVariant = outlineVariantDark,
	scrim = scrimDark,
	inverseSurface = inverseSurfaceDark,
	inverseOnSurface = inverseOnSurfaceDark,
	inversePrimary = inversePrimaryDark,
	surfaceDim = surfaceDimDark,
	surfaceBright = surfaceBrightDark,
	surfaceContainerLowest = surfaceContainerLowestDark,
	surfaceContainerLow = surfaceContainerLowDark,
	surfaceContainer = surfaceContainerDark,
	surfaceContainerHigh = surfaceContainerHighDark,
	surfaceContainerHighest = surfaceContainerHighestDark,
)

private val mediumContrastLightColorScheme = lightColorScheme(
	primary = primaryLightMediumContrast,
	onPrimary = onPrimaryLightMediumContrast,
	primaryContainer = primaryContainerLightMediumContrast,
	onPrimaryContainer = onPrimaryContainerLightMediumContrast,
	secondary = secondaryLightMediumContrast,
	onSecondary = onSecondaryLightMediumContrast,
	secondaryContainer = secondaryContainerLightMediumContrast,
	onSecondaryContainer = onSecondaryContainerLightMediumContrast,
	tertiary = tertiaryLightMediumContrast,
	onTertiary = onTertiaryLightMediumContrast,
	tertiaryContainer = tertiaryContainerLightMediumContrast,
	onTertiaryContainer = onTertiaryContainerLightMediumContrast,
	error = errorLightMediumContrast,
	onError = onErrorLightMediumContrast,
	errorContainer = errorContainerLightMediumContrast,
	onErrorContainer = onErrorContainerLightMediumContrast,
	background = backgroundLightMediumContrast,
	onBackground = onBackgroundLightMediumContrast,
	surface = surfaceLightMediumContrast,
	onSurface = onSurfaceLightMediumContrast,
	surfaceVariant = surfaceVariantLightMediumContrast,
	onSurfaceVariant = onSurfaceVariantLightMediumContrast,
	outline = outlineLightMediumContrast,
	outlineVariant = outlineVariantLightMediumContrast,
	scrim = scrimLightMediumContrast,
	inverseSurface = inverseSurfaceLightMediumContrast,
	inverseOnSurface = inverseOnSurfaceLightMediumContrast,
	inversePrimary = inversePrimaryLightMediumContrast,
	surfaceDim = surfaceDimLightMediumContrast,
	surfaceBright = surfaceBrightLightMediumContrast,
	surfaceContainerLowest = surfaceContainerLowestLightMediumContrast,
	surfaceContainerLow = surfaceContainerLowLightMediumContrast,
	surfaceContainer = surfaceContainerLightMediumContrast,
	surfaceContainerHigh = surfaceContainerHighLightMediumContrast,
	surfaceContainerHighest = surfaceContainerHighestLightMediumContrast,
)

private val highContrastLightColorScheme = lightColorScheme(
	primary = primaryLightHighContrast,
	onPrimary = onPrimaryLightHighContrast,
	primaryContainer = primaryContainerLightHighContrast,
	onPrimaryContainer = onPrimaryContainerLightHighContrast,
	secondary = secondaryLightHighContrast,
	onSecondary = onSecondaryLightHighContrast,
	secondaryContainer = secondaryContainerLightHighContrast,
	onSecondaryContainer = onSecondaryContainerLightHighContrast,
	tertiary = tertiaryLightHighContrast,
	onTertiary = onTertiaryLightHighContrast,
	tertiaryContainer = tertiaryContainerLightHighContrast,
	onTertiaryContainer = onTertiaryContainerLightHighContrast,
	error = errorLightHighContrast,
	onError = onErrorLightHighContrast,
	errorContainer = errorContainerLightHighContrast,
	onErrorContainer = onErrorContainerLightHighContrast,
	background = backgroundLightHighContrast,
	onBackground = onBackgroundLightHighContrast,
	surface = surfaceLightHighContrast,
	onSurface = onSurfaceLightHighContrast,
	surfaceVariant = surfaceVariantLightHighContrast,
	onSurfaceVariant = onSurfaceVariantLightHighContrast,
	outline = outlineLightHighContrast,
	outlineVariant = outlineVariantLightHighContrast,
	scrim = scrimLightHighContrast,
	inverseSurface = inverseSurfaceLightHighContrast,
	inverseOnSurface = inverseOnSurfaceLightHighContrast,
	inversePrimary = inversePrimaryLightHighContrast,
	surfaceDim = surfaceDimLightHighContrast,
	surfaceBright = surfaceBrightLightHighContrast,
	surfaceContainerLowest = surfaceContainerLowestLightHighContrast,
	surfaceContainerLow = surfaceContainerLowLightHighContrast,
	surfaceContainer = surfaceContainerLightHighContrast,
	surfaceContainerHigh = surfaceContainerHighLightHighContrast,
	surfaceContainerHighest = surfaceContainerHighestLightHighContrast,
)

private val mediumContrastDarkColorScheme = darkColorScheme(
	primary = primaryDarkMediumContrast,
	onPrimary = onPrimaryDarkMediumContrast,
	primaryContainer = primaryContainerDarkMediumContrast,
	onPrimaryContainer = onPrimaryContainerDarkMediumContrast,
	secondary = secondaryDarkMediumContrast,
	onSecondary = onSecondaryDarkMediumContrast,
	secondaryContainer = secondaryContainerDarkMediumContrast,
	onSecondaryContainer = onSecondaryContainerDarkMediumContrast,
	tertiary = tertiaryDarkMediumContrast,
	onTertiary = onTertiaryDarkMediumContrast,
	tertiaryContainer = tertiaryContainerDarkMediumContrast,
	onTertiaryContainer = onTertiaryContainerDarkMediumContrast,
	error = errorDarkMediumContrast,
	onError = onErrorDarkMediumContrast,
	errorContainer = errorContainerDarkMediumContrast,
	onErrorContainer = onErrorContainerDarkMediumContrast,
	background = backgroundDarkMediumContrast,
	onBackground = onBackgroundDarkMediumContrast,
	surface = surfaceDarkMediumContrast,
	onSurface = onSurfaceDarkMediumContrast,
	surfaceVariant = surfaceVariantDarkMediumContrast,
	onSurfaceVariant = onSurfaceVariantDarkMediumContrast,
	outline = outlineDarkMediumContrast,
	outlineVariant = outlineVariantDarkMediumContrast,
	scrim = scrimDarkMediumContrast,
	inverseSurface = inverseSurfaceDarkMediumContrast,
	inverseOnSurface = inverseOnSurfaceDarkMediumContrast,
	inversePrimary = inversePrimaryDarkMediumContrast,
	surfaceDim = surfaceDimDarkMediumContrast,
	surfaceBright = surfaceBrightDarkMediumContrast,
	surfaceContainerLowest = surfaceContainerLowestDarkMediumContrast,
	surfaceContainerLow = surfaceContainerLowDarkMediumContrast,
	surfaceContainer = surfaceContainerDarkMediumContrast,
	surfaceContainerHigh = surfaceContainerHighDarkMediumContrast,
	surfaceContainerHighest = surfaceContainerHighestDarkMediumContrast,
)

private val highContrastDarkColorScheme = darkColorScheme(
	primary = primaryDarkHighContrast,
	onPrimary = onPrimaryDarkHighContrast,
	primaryContainer = primaryContainerDarkHighContrast,
	onPrimaryContainer = onPrimaryContainerDarkHighContrast,
	secondary = secondaryDarkHighContrast,
	onSecondary = onSecondaryDarkHighContrast,
	secondaryContainer = secondaryContainerDarkHighContrast,
	onSecondaryContainer = onSecondaryContainerDarkHighContrast,
	tertiary = tertiaryDarkHighContrast,
	onTertiary = onTertiaryDarkHighContrast,
	tertiaryContainer = tertiaryContainerDarkHighContrast,
	onTertiaryContainer = onTertiaryContainerDarkHighContrast,
	error = errorDarkHighContrast,
	onError = onErrorDarkHighContrast,
	errorContainer = errorContainerDarkHighContrast,
	onErrorContainer = onErrorContainerDarkHighContrast,
	background = backgroundDarkHighContrast,
	onBackground = onBackgroundDarkHighContrast,
	surface = surfaceDarkHighContrast,
	onSurface = onSurfaceDarkHighContrast,
	surfaceVariant = surfaceVariantDarkHighContrast,
	onSurfaceVariant = onSurfaceVariantDarkHighContrast,
	outline = outlineDarkHighContrast,
	outlineVariant = outlineVariantDarkHighContrast,
	scrim = scrimDarkHighContrast,
	inverseSurface = inverseSurfaceDarkHighContrast,
	inverseOnSurface = inverseOnSurfaceDarkHighContrast,
	inversePrimary = inversePrimaryDarkHighContrast,
	surfaceDim = surfaceDimDarkHighContrast,
	surfaceBright = surfaceBrightDarkHighContrast,
	surfaceContainerLowest = surfaceContainerLowestDarkHighContrast,
	surfaceContainerLow = surfaceContainerLowDarkHighContrast,
	surfaceContainer = surfaceContainerDarkHighContrast,
	surfaceContainerHigh = surfaceContainerHighDarkHighContrast,
	surfaceContainerHighest = surfaceContainerHighestDarkHighContrast,
)


val extendedLight = ExtendedColorScheme(
	blue = ColorFamily(
		blueLight,
		onBlueLight,
		blueContainerLight,
		onBlueContainerLight,
	),
	red = ColorFamily(
		redLight,
		onRedLight,
		redContainerLight,
		onRedContainerLight,
	),
	green = ColorFamily(
		greenLight,
		onGreenLight,
		greenContainerLight,
		onGreenContainerLight,
	),
	yellow = ColorFamily(
		yellowLight,
		onYellowLight,
		yellowContainerLight,
		onYellowContainerLight,
	)
)

val extendedDark = ExtendedColorScheme(
	blue = ColorFamily(
		blueDark,
		onBlueDark,
		blueContainerDark,
		onBlueContainerDark,
	),
	red = ColorFamily(
		redDark,
		onRedDark,
		redContainerDark,
		onRedContainerDark,
	),
	green = ColorFamily(
		greenDark,
		onGreenDark,
		greenContainerDark,
		onGreenContainerDark,
	),
	yellow = ColorFamily(
		yellowDark,
		onYellowDark,
		yellowContainerDark,
		onYellowContainerDark,
	),
)

val extendedLightMediumContrast = ExtendedColorScheme(
	blue = ColorFamily(
		blueLightMediumContrast,
		onBlueLightMediumContrast,
		blueContainerLightMediumContrast,
		onBlueContainerLightMediumContrast,
	),
	red = ColorFamily(
		redLightMediumContrast,
		onRedLightMediumContrast,
		redContainerLightMediumContrast,
		onRedContainerLightMediumContrast,
	),
	green = ColorFamily(
		greenLightMediumContrast,
		onGreenLightMediumContrast,
		greenContainerLightMediumContrast,
		onGreenContainerLightMediumContrast,
	),
	yellow = ColorFamily(
		yellowLightMediumContrast,
		onYellowLightMediumContrast,
		yellowContainerLightMediumContrast,
		onYellowContainerLightMediumContrast,
	),
)

val extendedLightHighContrast = ExtendedColorScheme(
	blue = ColorFamily(
		blueLightHighContrast,
		onBlueLightHighContrast,
		blueContainerLightHighContrast,
		onBlueContainerLightHighContrast,
	),
	red = ColorFamily(
		redLightHighContrast,
		onRedLightHighContrast,
		redContainerLightHighContrast,
		onRedContainerLightHighContrast,
	),
	green = ColorFamily(
		greenLightHighContrast,
		onGreenLightHighContrast,
		greenContainerLightHighContrast,
		onGreenContainerLightHighContrast,
	),
	yellow = ColorFamily(
		yellowLightHighContrast,
		onYellowLightHighContrast,
		yellowContainerLightHighContrast,
		onYellowContainerLightHighContrast,
	),
)

val extendedDarkMediumContrast = ExtendedColorScheme(
	blue = ColorFamily(
		blueDarkMediumContrast,
		onBlueDarkMediumContrast,
		blueContainerDarkMediumContrast,
		onBlueContainerDarkMediumContrast,
	),
	red = ColorFamily(
		redDarkMediumContrast,
		onRedDarkMediumContrast,
		redContainerDarkMediumContrast,
		onRedContainerDarkMediumContrast,
	),
	green = ColorFamily(
		greenDarkMediumContrast,
		onGreenDarkMediumContrast,
		greenContainerDarkMediumContrast,
		onGreenContainerDarkMediumContrast,
	),
	yellow = ColorFamily(
		yellowDarkMediumContrast,
		onYellowDarkMediumContrast,
		yellowContainerDarkMediumContrast,
		onYellowContainerDarkMediumContrast,
	),
)

val extendedDarkHighContrast = ExtendedColorScheme(
	blue = ColorFamily(
		blueDarkHighContrast,
		onBlueDarkHighContrast,
		blueContainerDarkHighContrast,
		onBlueContainerDarkHighContrast,
	),
	red = ColorFamily(
		redDarkHighContrast,
		onRedDarkHighContrast,
		redContainerDarkHighContrast,
		onRedContainerDarkHighContrast,
	),
	green = ColorFamily(
		greenDarkHighContrast,
		onGreenDarkHighContrast,
		greenContainerDarkHighContrast,
		onGreenContainerDarkHighContrast,
	),
	yellow = ColorFamily(
		yellowDarkHighContrast,
		onYellowDarkHighContrast,
		yellowContainerDarkHighContrast,
		onYellowContainerDarkHighContrast,
	),
)

@Immutable
data class ColorFamily(
	val color: Color,
	val onColor: Color,
	val colorContainer: Color,
	val onColorContainer: Color
)

val unspecified_scheme = ColorFamily(
	Color.Unspecified, Color.Unspecified, Color.Unspecified, Color.Unspecified
)


val LocalExtendedColors = staticCompositionLocalOf {
	ExtendedColorScheme(
		blue = unspecified_scheme,
		red = unspecified_scheme,
		green = unspecified_scheme,
		yellow = unspecified_scheme
	)
}
val MaterialTheme.extendedColorScheme: ExtendedColorScheme
	@Composable
	@ReadOnlyComposable
	get() = LocalExtendedColors.current

@Composable
fun SyncthingandroidTheme(
	darkTheme: Boolean? = null,
	// Dynamic color is available on Android 12+
	dynamicColor: Boolean = true,
	pureBlack: Boolean = ThemeControls.pureBlack,
	content: @Composable () -> Unit
) {
	var darkTheme = darkTheme
	if (darkTheme == null) darkTheme = isSystemInDarkTheme()

	var colorScheme = when {
		dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
			val context = LocalContext.current
			if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
		}

		darkTheme -> darkScheme
		else -> lightScheme
	}

	if (darkTheme && pureBlack) {
		colorScheme = colorScheme.copy(
			surface = Color.Black,
			background = Color.Black
		)
	}

	val extendedColors = if (darkTheme) extendedDark else extendedLight

	CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
		MaterialTheme(
			colorScheme = colorScheme,
			typography = AppTypography,
			content = content
		)
	}
}

