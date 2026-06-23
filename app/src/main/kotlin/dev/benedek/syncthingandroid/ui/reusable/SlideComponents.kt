package dev.benedek.syncthingandroid.ui.reusable

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.ui.LocalIsLandscape
import dev.benedek.syncthingandroid.ui.theme.SyncthingandroidTheme
import dev.benedek.syncthingandroid.util.ThemeControls
import kotlin.math.exp


@SuppressLint("ModifierParameter")
@Composable
fun SlideWelcomeTitle(
	text: String,
	modifier: Modifier = Modifier.padding(30.dp),
	fontSize: TextUnit = 30.sp,
	textAlign: TextAlign? = TextAlign.Center,
	lineHeight: TextUnit = 30.sp
) {
	Text(
		text = text,
		modifier = modifier,
		fontSize = fontSize,
		textAlign = textAlign,
		lineHeight = lineHeight
	)
}

@Composable
fun SlideTitle(
	text: String,
	modifier: Modifier = Modifier,
	fontSize: TextUnit = dimensionResource(R.dimen.slide_title).value.sp,
	fontWeight: FontWeight = FontWeight.Bold,
	textAlign: TextAlign? = TextAlign.Center
) {
	Text(
		text = text,
		modifier = modifier,
		fontSize = fontSize,
		fontWeight = fontWeight,
		textAlign = textAlign
	)
}

enum class TextLayout {
	Fixed,
	Expandable
}

@Composable
fun SlideDescription(
	subtitle: AnnotatedString,
	description: AnnotatedString?,
	modifier: Modifier = Modifier,
	textAlign: TextAlign = TextAlign.Center,
	fontSize: TextUnit = dimensionResource(R.dimen.slide_desc).value.sp,
	lineHeight: TextUnit = 16.sp,
	textLayout: TextLayout = TextLayout.Fixed
	) {

	var expanded by retain { mutableStateOf(textLayout == TextLayout.Fixed) }


	Column(horizontalAlignment = Alignment.CenterHorizontally) {
		Text(
			text = subtitle,
			modifier = modifier,
			textAlign = textAlign,
			fontSize = fontSize,
			lineHeight = lineHeight,
		)
		if (description != null) {
			AnimatedVisibility(
				visible = expanded,
				enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
				exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
			) {
				Text(
					text = description,
					modifier = modifier,
					textAlign = textAlign,
					fontSize = fontSize,
					lineHeight = lineHeight,
				)
			}
			if (textLayout == TextLayout.Expandable) {
				IconButton(
					onClick = { expanded = !expanded }
				) {
					Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null)
				}
			}
		}

	}
}

@Composable
fun SlideDescription(
	subtitle: String,
	description: String?,
	modifier: Modifier = Modifier,
	textAlign: TextAlign = TextAlign.Center,
	fontSize: TextUnit = dimensionResource(R.dimen.slide_desc).value.sp,
	lineHeight: TextUnit = 16.sp,
	textLayout: TextLayout = TextLayout.Fixed
) {
	SlideDescription(
		AnnotatedString(subtitle),
		description.let { if (it == null) null else AnnotatedString(it) },
		modifier,
		textAlign,
		fontSize,
		lineHeight,
		textLayout
	)
}

@Composable
fun SlideImage(
	painter: Painter,
	modifier: Modifier = Modifier,
	contentDescription: String? = null,
	colorFilter: ColorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
) {
	Image(
		painter,
		contentDescription,
		modifier
			.size(dimensionResource(R.dimen.img_width_height)),
		contentScale = ContentScale.Fit,
		colorFilter = colorFilter
	)
}

@Composable
fun AdaptiveSlideLayout(
	title: @Composable () -> Unit,
	description: @Composable () -> Unit,
	modifier: Modifier = Modifier,
	image: (@Composable () -> Unit)? = null,
	action: (@Composable () -> Unit)? = null,
) {
	val isLandscape = LocalIsLandscape.current
	val scrollState = rememberScrollState()

	if (isLandscape) {
		Row(
			modifier = modifier
				.fillMaxSize()
				.padding(horizontal = 32.dp, vertical = 8.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.Center
		) {
			if (image != null) {
				Box(
					modifier = Modifier
						.weight(0.8f)
						.padding(16.dp),
					contentAlignment = Alignment.Center
				) {
					image()
				}
			}
			Column(
				modifier = Modifier
					.weight(1.2f)
					.fillMaxSize()
					.verticalScroll(scrollState)
					.padding(horizontal = 16.dp),
				horizontalAlignment = Alignment.CenterHorizontally,
				verticalArrangement = Arrangement.Center
			) {
				title()
				Spacer(modifier = Modifier.height(8.dp))
				description()
				if (action != null) {
					Spacer(modifier = Modifier.height(16.dp))
					action()
				}
			}
		}
	} else {
		Column(
			modifier = modifier
				.fillMaxSize()
				.verticalScroll(scrollState)
				.padding(horizontal = 24.dp),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.Center
		) {
			if (image != null) {
				image()
				Spacer(modifier = Modifier.height(32.dp))
			}
			title()
			Spacer(modifier = Modifier.height(4.dp))
			description()
			if (action != null) {
				Spacer(modifier = Modifier.height(24.dp))
				action()
			}
		}
	}
}


@Preview(uiMode = ThemeControls.UI_MODE)
@Composable
fun SlideWelcomeTitlePreview() {
	SyncthingandroidTheme(ThemeControls.PREVIEW_DARK_THEME, ThemeControls.isMonetEnabled) {
		Surface { SlideWelcomeTitle("Welcome to Syncthing") }
	}
}

@Preview(uiMode = ThemeControls.UI_MODE)
@Composable
fun SlideTitlePreview() {
	SyncthingandroidTheme(ThemeControls.PREVIEW_DARK_THEME, ThemeControls.isMonetEnabled) {
		Surface { SlideTitle("Introduction") }
	}
}

@Preview(uiMode = ThemeControls.UI_MODE)
@Composable
fun SlideDescriptionPreview() {
	SyncthingandroidTheme(ThemeControls.PREVIEW_DARK_THEME, ThemeControls.isMonetEnabled) {
		Surface {
			SlideDescription(
				"Syncthing replaces proprietary cloud and data services with something open, trustworthy and decentralized.",
				"To share data with other devices, you need to add their unique device IDs to the device list. Afterwards you can select which folders to share with which devices.\n\nPlease report any problems you encounter via GitHub."
			)
		}
	}
}

@Preview(uiMode = ThemeControls.UI_MODE)
@Composable
fun SlideImagePreview() {
	SyncthingandroidTheme(ThemeControls.PREVIEW_DARK_THEME, ThemeControls.isMonetEnabled) {
		Surface { SlideImage(painterResource(R.drawable.ic_launcher_monochrome)) }
	}
}

@Preview(uiMode = ThemeControls.UI_MODE, name = "Portrait")
@Composable
fun AdaptiveSlideLayoutPortraitPreview() {
	SyncthingandroidTheme(ThemeControls.PREVIEW_DARK_THEME, ThemeControls.isMonetEnabled) {
		Surface {
			AdaptiveSlideLayout(
				title = { SlideTitle("Introduction") },
				description = { SlideDescription("Syncthing replaces proprietary cloud and data services with something open, trustworthy and decentralized.", "") },
				image = { SlideImage(painterResource(R.drawable.ic_launcher_monochrome)) }
			)
		}
	}
}

@Preview(uiMode = ThemeControls.UI_MODE, name = "Landscape", widthDp = 800, heightDp = 400)
@Composable
fun AdaptiveSlideLayoutLandscapePreview() {
	SyncthingandroidTheme(ThemeControls.PREVIEW_DARK_THEME, ThemeControls.isMonetEnabled) {
		CompositionLocalProvider(LocalIsLandscape provides true) {
			Surface {
				AdaptiveSlideLayout(
					title = { SlideTitle("Introduction") },
					description = { SlideDescription("Syncthing replaces proprietary cloud and data services with something open, trustworthy and decentralized.", "") },
					image = { SlideImage(painterResource(R.drawable.ic_launcher_monochrome)) }
				)
			}
		}
	}
}
