@file:Suppress("Unused", "ConstPropertyName")

package dev.benedek.syncthingandroid.ui.reusable

/*
 * I had enough of Google making recommended material style values private, so I made this file
 * to address the problem.
 */

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp


enum class ColorSchemeKeyTokens {
	Background,
	Error,
	ErrorContainer,
	InverseOnSurface,
	InversePrimary,
	InverseSurface,
	OnBackground,
	OnError,
	OnErrorContainer,
	OnPrimary,
	OnPrimaryContainer,
	OnPrimaryFixed,
	OnPrimaryFixedVariant,
	OnSecondary,
	OnSecondaryContainer,
	OnSecondaryFixed,
	OnSecondaryFixedVariant,
	OnSurface,
	OnSurfaceVariant,
	OnTertiary,
	OnTertiaryContainer,
	OnTertiaryFixed,
	OnTertiaryFixedVariant,
	Outline,
	OutlineVariant,
	Primary,
	PrimaryContainer,
	PrimaryFixed,
	PrimaryFixedDim,
	Scrim,
	Secondary,
	SecondaryContainer,
	SecondaryFixed,
	SecondaryFixedDim,
	Surface,
	SurfaceBright,
	SurfaceContainer,
	SurfaceContainerHigh,
	SurfaceContainerHighest,
	SurfaceContainerLow,
	SurfaceContainerLowest,
	SurfaceDim,
	SurfaceTint,
	SurfaceVariant,
	Tertiary,
	TertiaryContainer,
	TertiaryFixed,
	TertiaryFixedDim,
}
@Immutable
class ColorScheme(
	val primary: Color,
	val onPrimary: Color,
	val primaryContainer: Color,
	val onPrimaryContainer: Color,
	val inversePrimary: Color,
	val secondary: Color,
	val onSecondary: Color,
	val secondaryContainer: Color,
	val onSecondaryContainer: Color,
	val tertiary: Color,
	val onTertiary: Color,
	val tertiaryContainer: Color,
	val onTertiaryContainer: Color,
	val background: Color,
	val onBackground: Color,
	val surface: Color,
	val onSurface: Color,
	val surfaceVariant: Color,
	val onSurfaceVariant: Color,
	val surfaceTint: Color,
	val inverseSurface: Color,
	val inverseOnSurface: Color,
	val error: Color,
	val onError: Color,
	val errorContainer: Color,
	val onErrorContainer: Color,
	val outline: Color,
	val outlineVariant: Color,
	val scrim: Color,
	val surfaceBright: Color,
	val surfaceDim: Color,
	val surfaceContainer: Color,
	val surfaceContainerHigh: Color,
	val surfaceContainerHighest: Color,
	val surfaceContainerLow: Color,
	val surfaceContainerLowest: Color,
	val primaryFixed: Color,
	val primaryFixedDim: Color,
	val onPrimaryFixed: Color,
	val onPrimaryFixedVariant: Color,
	val secondaryFixed: Color,
	val secondaryFixedDim: Color,
	val onSecondaryFixed: Color,
	val onSecondaryFixedVariant: Color,
	val tertiaryFixed: Color,
	val tertiaryFixedDim: Color,
	val onTertiaryFixed: Color,
	val onTertiaryFixedVariant: Color,
) {
	/** Returns a copy of this ColorScheme, optionally overriding some of the values. */
	fun copy(
		primary: Color = this.primary,
		onPrimary: Color = this.onPrimary,
		primaryContainer: Color = this.primaryContainer,
		onPrimaryContainer: Color = this.onPrimaryContainer,
		inversePrimary: Color = this.inversePrimary,
		secondary: Color = this.secondary,
		onSecondary: Color = this.onSecondary,
		secondaryContainer: Color = this.secondaryContainer,
		onSecondaryContainer: Color = this.onSecondaryContainer,
		tertiary: Color = this.tertiary,
		onTertiary: Color = this.onTertiary,
		tertiaryContainer: Color = this.tertiaryContainer,
		onTertiaryContainer: Color = this.onTertiaryContainer,
		background: Color = this.background,
		onBackground: Color = this.onBackground,
		surface: Color = this.surface,
		onSurface: Color = this.onSurface,
		surfaceVariant: Color = this.surfaceVariant,
		onSurfaceVariant: Color = this.onSurfaceVariant,
		surfaceTint: Color = this.surfaceTint,
		inverseSurface: Color = this.inverseSurface,
		inverseOnSurface: Color = this.inverseOnSurface,
		error: Color = this.error,
		onError: Color = this.onError,
		errorContainer: Color = this.errorContainer,
		onErrorContainer: Color = this.onErrorContainer,
		outline: Color = this.outline,
		outlineVariant: Color = this.outlineVariant,
		scrim: Color = this.scrim,
		surfaceBright: Color = this.surfaceBright,
		surfaceDim: Color = this.surfaceDim,
		surfaceContainer: Color = this.surfaceContainer,
		surfaceContainerHigh: Color = this.surfaceContainerHigh,
		surfaceContainerHighest: Color = this.surfaceContainerHighest,
		surfaceContainerLow: Color = this.surfaceContainerLow,
		surfaceContainerLowest: Color = this.surfaceContainerLowest,
		primaryFixed: Color = this.primaryFixed,
		primaryFixedDim: Color = this.primaryFixedDim,
		onPrimaryFixed: Color = this.onPrimaryFixed,
		onPrimaryFixedVariant: Color = this.onPrimaryFixedVariant,
		secondaryFixed: Color = this.secondaryFixed,
		secondaryFixedDim: Color = this.secondaryFixedDim,
		onSecondaryFixed: Color = this.onSecondaryFixed,
		onSecondaryFixedVariant: Color = this.onSecondaryFixedVariant,
		tertiaryFixed: Color = this.tertiaryFixed,
		tertiaryFixedDim: Color = this.tertiaryFixedDim,
		onTertiaryFixed: Color = this.onTertiaryFixed,
		onTertiaryFixedVariant: Color = this.onTertiaryFixedVariant,
	): ColorScheme =
		ColorScheme(
			primary = primary,
			onPrimary = onPrimary,
			primaryContainer = primaryContainer,
			onPrimaryContainer = onPrimaryContainer,
			inversePrimary = inversePrimary,
			secondary = secondary,
			onSecondary = onSecondary,
			secondaryContainer = secondaryContainer,
			onSecondaryContainer = onSecondaryContainer,
			tertiary = tertiary,
			onTertiary = onTertiary,
			tertiaryContainer = tertiaryContainer,
			onTertiaryContainer = onTertiaryContainer,
			background = background,
			onBackground = onBackground,
			surface = surface,
			onSurface = onSurface,
			surfaceVariant = surfaceVariant,
			onSurfaceVariant = onSurfaceVariant,
			surfaceTint = surfaceTint,
			inverseSurface = inverseSurface,
			inverseOnSurface = inverseOnSurface,
			error = error,
			onError = onError,
			errorContainer = errorContainer,
			onErrorContainer = onErrorContainer,
			outline = outline,
			outlineVariant = outlineVariant,
			scrim = scrim,
			surfaceBright = surfaceBright,
			surfaceDim = surfaceDim,
			surfaceContainer = surfaceContainer,
			surfaceContainerHigh = surfaceContainerHigh,
			surfaceContainerHighest = surfaceContainerHighest,
			surfaceContainerLow = surfaceContainerLow,
			surfaceContainerLowest = surfaceContainerLowest,
			primaryFixed = primaryFixed,
			primaryFixedDim = primaryFixedDim,
			onPrimaryFixed = onPrimaryFixed,
			onPrimaryFixedVariant = onPrimaryFixedVariant,
			secondaryFixed = secondaryFixed,
			secondaryFixedDim = secondaryFixedDim,
			onSecondaryFixed = onSecondaryFixed,
			onSecondaryFixedVariant = onSecondaryFixedVariant,
			tertiaryFixed = tertiaryFixed,
			tertiaryFixedDim = tertiaryFixedDim,
			onTertiaryFixed = onTertiaryFixed,
			onTertiaryFixedVariant = onTertiaryFixedVariant,
		)

	override fun toString(): String {
		return "ColorScheme(" +
				"primary=$primary" +
				"onPrimary=$onPrimary" +
				"primaryContainer=$primaryContainer" +
				"onPrimaryContainer=$onPrimaryContainer" +
				"inversePrimary=$inversePrimary" +
				"secondary=$secondary" +
				"onSecondary=$onSecondary" +
				"secondaryContainer=$secondaryContainer" +
				"onSecondaryContainer=$onSecondaryContainer" +
				"tertiary=$tertiary" +
				"onTertiary=$onTertiary" +
				"tertiaryContainer=$tertiaryContainer" +
				"onTertiaryContainer=$onTertiaryContainer" +
				"background=$background" +
				"onBackground=$onBackground" +
				"surface=$surface" +
				"onSurface=$onSurface" +
				"surfaceVariant=$surfaceVariant" +
				"onSurfaceVariant=$onSurfaceVariant" +
				"surfaceTint=$surfaceTint" +
				"inverseSurface=$inverseSurface" +
				"inverseOnSurface=$inverseOnSurface" +
				"error=$error" +
				"onError=$onError" +
				"errorContainer=$errorContainer" +
				"onErrorContainer=$onErrorContainer" +
				"outline=$outline" +
				"outlineVariant=$outlineVariant" +
				"scrim=$scrim" +
				"surfaceBright=$surfaceBright" +
				"surfaceDim=$surfaceDim" +
				"surfaceContainer=$surfaceContainer" +
				"surfaceContainerHigh=$surfaceContainerHigh" +
				"surfaceContainerHighest=$surfaceContainerHighest" +
				"surfaceContainerLow=$surfaceContainerLow" +
				"surfaceContainerLowest=$surfaceContainerLowest" +
				"primaryFixed=$primaryFixed" +
				"primaryFixedDim=$primaryFixedDim" +
				"onPrimaryFixed=$onPrimaryContainer" +
				"onPrimaryFixedVariant=$onPrimaryFixedVariant" +
				"secondaryFixed=$secondaryFixed" +
				"secondaryFixedDim=$secondaryFixedDim" +
				"onSecondaryFixed=$onSecondaryFixed" +
				"onSecondaryFixedVariant=$onSecondaryFixedVariant" +
				"tertiaryFixed=$tertiaryFixed" +
				"tertiaryFixedDim=$tertiaryFixedDim" +
				"onTertiaryFixed=$onTertiaryFixed" +
				"onTertiaryFixedVariant=$onTertiaryFixedVariant" +
				")"
	}
}

enum class ShapeKeyTokens {
	CornerExtraExtraLarge,
	CornerExtraLarge,
	CornerExtraLargeIncreased,
	CornerExtraLargeTop,
	CornerExtraSmall,
	CornerExtraSmallTop,
	CornerFull,
	CornerLarge,
	CornerLargeEnd,
	CornerLargeIncreased,
	CornerLargeStart,
	CornerLargeTop,
	CornerMedium,
	CornerNone,
	CornerSmall,
}
object ButtonSmallTokens {
	val ContainerHeight = 40.0.dp
	val ContainerShapeRound = ShapeKeyTokens.CornerFull
	val ContainerShapeSquare = ShapeKeyTokens.CornerMedium
	val IconLabelSpace = 8.0.dp
	val IconSize = 20.0.dp
	val LeadingSpace = 16.0.dp
	val OutlinedOutlineWidth = 1.0.dp
	val PressedContainerShape = ShapeKeyTokens.CornerSmall
	val SelectedContainerShapeRound = ShapeKeyTokens.CornerFull
	val SelectedContainerShapeSquare = ShapeKeyTokens.CornerMedium
	val TrailingSpace = 16.0.dp
}

object OutlinedButtonTokens {
	const val DisabledContainerOpacity = 0.1f
	val DisabledIconColor = ColorSchemeKeyTokens.OnSurfaceVariant
	const val DisabledIconOpacity = 0.38f
	val DisabledLabelTextColor = ColorSchemeKeyTokens.OnSurfaceVariant
	const val DisabledLabelTextOpacity = 0.38f
	val DisabledOutlineColor = ColorSchemeKeyTokens.OutlineVariant
	val FocusedIconColor = ColorSchemeKeyTokens.OnSurfaceVariant
	val FocusedLabelTextColor = ColorSchemeKeyTokens.OnSurfaceVariant
	val FocusedOutlineColor = ColorSchemeKeyTokens.OutlineVariant
	val HoveredIconColor = ColorSchemeKeyTokens.OnSurfaceVariant
	val HoveredLabelTextColor = ColorSchemeKeyTokens.OnSurfaceVariant
	val HoveredOutlineColor = ColorSchemeKeyTokens.OutlineVariant
	val IconColor = ColorSchemeKeyTokens.OnSurfaceVariant
	val LabelTextColor = ColorSchemeKeyTokens.OnSurfaceVariant
	val OutlineColor = ColorSchemeKeyTokens.OutlineVariant
	val PressedIconColor = ColorSchemeKeyTokens.OnSurfaceVariant
	val PressedLabelTextColor = ColorSchemeKeyTokens.OnSurfaceVariant
	val PressedOutlineColor = ColorSchemeKeyTokens.OutlineVariant
	val SelectedContainerColor = ColorSchemeKeyTokens.InverseSurface
	val SelectedDisabledContainerColor = ColorSchemeKeyTokens.OnSurface
	val SelectedFocusedIconColor = ColorSchemeKeyTokens.InverseOnSurface
	val SelectedFocusedLabelTextColor = ColorSchemeKeyTokens.InverseOnSurface
	val SelectedHoveredIconColor = ColorSchemeKeyTokens.InverseOnSurface
	val SelectedHoveredLabelTextColor = ColorSchemeKeyTokens.InverseOnSurface
	val SelectedIconColor = ColorSchemeKeyTokens.InverseOnSurface
	val SelectedLabelTextColor = ColorSchemeKeyTokens.InverseOnSurface
	val SelectedPressedIconColor = ColorSchemeKeyTokens.InverseOnSurface
	val SelectedPressedLabelTextColor = ColorSchemeKeyTokens.InverseOnSurface
	val UnselectedDisabledOutlineColor = ColorSchemeKeyTokens.OutlineVariant
	val UnselectedFocusedIconColor = ColorSchemeKeyTokens.OnSurfaceVariant
	val UnselectedFocusedLabelTextColor = ColorSchemeKeyTokens.OnSurfaceVariant
	val UnselectedFocusedOutlineColor = ColorSchemeKeyTokens.OutlineVariant
	val UnselectedHoveredIconColor = ColorSchemeKeyTokens.OnSurfaceVariant
	val UnselectedHoveredLabelTextColor = ColorSchemeKeyTokens.OnSurfaceVariant
	val UnselectedHoveredOutlineColor = ColorSchemeKeyTokens.OutlineVariant
	val UnselectedIconColor = ColorSchemeKeyTokens.OnSurfaceVariant
	val UnselectedLabelTextColor = ColorSchemeKeyTokens.OnSurfaceVariant
	val UnselectedPressedIconColor = ColorSchemeKeyTokens.OnSurfaceVariant
	val UnselectedPressedLabelTextColor = ColorSchemeKeyTokens.OnSurfaceVariant
	val UnselectedPressedOutlineColor = ColorSchemeKeyTokens.OutlineVariant
}
val ColorSchemeKeyTokens.value: Color
	@ReadOnlyComposable @Composable get() = MaterialTheme.colorScheme.fromToken(this)

@Stable
fun androidx.compose.material3.ColorScheme.fromToken(value: ColorSchemeKeyTokens): Color {
	return when (value) {
		ColorSchemeKeyTokens.Background -> background
		ColorSchemeKeyTokens.Error -> error
		ColorSchemeKeyTokens.ErrorContainer -> errorContainer
		ColorSchemeKeyTokens.InverseOnSurface -> inverseOnSurface
		ColorSchemeKeyTokens.InversePrimary -> inversePrimary
		ColorSchemeKeyTokens.InverseSurface -> inverseSurface
		ColorSchemeKeyTokens.OnBackground -> onBackground
		ColorSchemeKeyTokens.OnError -> onError
		ColorSchemeKeyTokens.OnErrorContainer -> onErrorContainer
		ColorSchemeKeyTokens.OnPrimary -> onPrimary
		ColorSchemeKeyTokens.OnPrimaryContainer -> onPrimaryContainer
		ColorSchemeKeyTokens.OnSecondary -> onSecondary
		ColorSchemeKeyTokens.OnSecondaryContainer -> onSecondaryContainer
		ColorSchemeKeyTokens.OnSurface -> onSurface
		ColorSchemeKeyTokens.OnSurfaceVariant -> onSurfaceVariant
		ColorSchemeKeyTokens.SurfaceTint -> surfaceTint
		ColorSchemeKeyTokens.OnTertiary -> onTertiary
		ColorSchemeKeyTokens.OnTertiaryContainer -> onTertiaryContainer
		ColorSchemeKeyTokens.Outline -> outline
		ColorSchemeKeyTokens.OutlineVariant -> outlineVariant
		ColorSchemeKeyTokens.Primary -> primary
		ColorSchemeKeyTokens.PrimaryContainer -> primaryContainer
		ColorSchemeKeyTokens.Scrim -> scrim
		ColorSchemeKeyTokens.Secondary -> secondary
		ColorSchemeKeyTokens.SecondaryContainer -> secondaryContainer
		ColorSchemeKeyTokens.Surface -> surface
		ColorSchemeKeyTokens.SurfaceVariant -> surfaceVariant
		ColorSchemeKeyTokens.SurfaceBright -> surfaceBright
		ColorSchemeKeyTokens.SurfaceContainer -> surfaceContainer
		ColorSchemeKeyTokens.SurfaceContainerHigh -> surfaceContainerHigh
		ColorSchemeKeyTokens.SurfaceContainerHighest -> surfaceContainerHighest
		ColorSchemeKeyTokens.SurfaceContainerLow -> surfaceContainerLow
		ColorSchemeKeyTokens.SurfaceContainerLowest -> surfaceContainerLowest
		ColorSchemeKeyTokens.SurfaceDim -> surfaceDim
		ColorSchemeKeyTokens.Tertiary -> tertiary
		ColorSchemeKeyTokens.TertiaryContainer -> tertiaryContainer
		ColorSchemeKeyTokens.PrimaryFixed -> primaryFixed
		ColorSchemeKeyTokens.PrimaryFixedDim -> primaryFixedDim
		ColorSchemeKeyTokens.OnPrimaryFixed -> onPrimaryFixed
		ColorSchemeKeyTokens.OnPrimaryFixedVariant -> onPrimaryFixedVariant
		ColorSchemeKeyTokens.SecondaryFixed -> secondaryFixed
		ColorSchemeKeyTokens.SecondaryFixedDim -> secondaryFixedDim
		ColorSchemeKeyTokens.OnSecondaryFixed -> onSecondaryFixed
		ColorSchemeKeyTokens.OnSecondaryFixedVariant -> onSecondaryFixedVariant
		ColorSchemeKeyTokens.TertiaryFixed -> tertiaryFixed
		ColorSchemeKeyTokens.TertiaryFixedDim -> tertiaryFixedDim
		ColorSchemeKeyTokens.OnTertiaryFixed -> onTertiaryFixed
		ColorSchemeKeyTokens.OnTertiaryFixedVariant -> onTertiaryFixedVariant
	}
}

@Stable
fun ColorScheme.fromToken(value: ColorSchemeKeyTokens): Color {
	return when (value) {
		ColorSchemeKeyTokens.Background -> background
		ColorSchemeKeyTokens.Error -> error
		ColorSchemeKeyTokens.ErrorContainer -> errorContainer
		ColorSchemeKeyTokens.InverseOnSurface -> inverseOnSurface
		ColorSchemeKeyTokens.InversePrimary -> inversePrimary
		ColorSchemeKeyTokens.InverseSurface -> inverseSurface
		ColorSchemeKeyTokens.OnBackground -> onBackground
		ColorSchemeKeyTokens.OnError -> onError
		ColorSchemeKeyTokens.OnErrorContainer -> onErrorContainer
		ColorSchemeKeyTokens.OnPrimary -> onPrimary
		ColorSchemeKeyTokens.OnPrimaryContainer -> onPrimaryContainer
		ColorSchemeKeyTokens.OnSecondary -> onSecondary
		ColorSchemeKeyTokens.OnSecondaryContainer -> onSecondaryContainer
		ColorSchemeKeyTokens.OnSurface -> onSurface
		ColorSchemeKeyTokens.OnSurfaceVariant -> onSurfaceVariant
		ColorSchemeKeyTokens.SurfaceTint -> surfaceTint
		ColorSchemeKeyTokens.OnTertiary -> onTertiary
		ColorSchemeKeyTokens.OnTertiaryContainer -> onTertiaryContainer
		ColorSchemeKeyTokens.Outline -> outline
		ColorSchemeKeyTokens.OutlineVariant -> outlineVariant
		ColorSchemeKeyTokens.Primary -> primary
		ColorSchemeKeyTokens.PrimaryContainer -> primaryContainer
		ColorSchemeKeyTokens.Scrim -> scrim
		ColorSchemeKeyTokens.Secondary -> secondary
		ColorSchemeKeyTokens.SecondaryContainer -> secondaryContainer
		ColorSchemeKeyTokens.Surface -> surface
		ColorSchemeKeyTokens.SurfaceVariant -> surfaceVariant
		ColorSchemeKeyTokens.SurfaceBright -> surfaceBright
		ColorSchemeKeyTokens.SurfaceContainer -> surfaceContainer
		ColorSchemeKeyTokens.SurfaceContainerHigh -> surfaceContainerHigh
		ColorSchemeKeyTokens.SurfaceContainerHighest -> surfaceContainerHighest
		ColorSchemeKeyTokens.SurfaceContainerLow -> surfaceContainerLow
		ColorSchemeKeyTokens.SurfaceContainerLowest -> surfaceContainerLowest
		ColorSchemeKeyTokens.SurfaceDim -> surfaceDim
		ColorSchemeKeyTokens.Tertiary -> tertiary
		ColorSchemeKeyTokens.TertiaryContainer -> tertiaryContainer
		ColorSchemeKeyTokens.PrimaryFixed -> primaryFixed
		ColorSchemeKeyTokens.PrimaryFixedDim -> primaryFixedDim
		ColorSchemeKeyTokens.OnPrimaryFixed -> onPrimaryFixed
		ColorSchemeKeyTokens.OnPrimaryFixedVariant -> onPrimaryFixedVariant
		ColorSchemeKeyTokens.SecondaryFixed -> secondaryFixed
		ColorSchemeKeyTokens.SecondaryFixedDim -> secondaryFixedDim
		ColorSchemeKeyTokens.OnSecondaryFixed -> onSecondaryFixed
		ColorSchemeKeyTokens.OnSecondaryFixedVariant -> onSecondaryFixedVariant
		ColorSchemeKeyTokens.TertiaryFixed -> tertiaryFixed
		ColorSchemeKeyTokens.TertiaryFixedDim -> tertiaryFixedDim
		ColorSchemeKeyTokens.OnTertiaryFixed -> onTertiaryFixed
		ColorSchemeKeyTokens.OnTertiaryFixedVariant -> onTertiaryFixedVariant
	}
}

