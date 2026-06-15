package dev.benedek.syncthingandroid.ui.reusable

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.ui.theme.SyncthingandroidTheme
import dev.benedek.syncthingandroid.util.ThemeControls


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

@SuppressLint("ModifierParameter")
@Composable
fun SlideTitle(
	text: String,
	modifier: Modifier = Modifier,
	fontSize: TextUnit = dimensionResource(R.dimen.slide_title).value.sp,
	fontWeight: FontWeight = FontWeight.Bold,
	textAlign: TextAlign? = TextAlign.Center
) {
	Text(text = text, fontSize = fontSize, fontWeight = fontWeight, textAlign = textAlign)
}

@SuppressLint("ModifierParameter")
@Composable
fun SlideDescription(
	text: String,
	modifier: Modifier = Modifier.padding(
		dimensionResource(R.dimen.desc_padding),
		dimensionResource(R.dimen.desc_marginTop),
		dimensionResource(R.dimen.desc_padding),
		dimensionResource(R.dimen.desc_paddingBottom)
	),
	textAlign: TextAlign = TextAlign.Center,
	fontSize: TextUnit = dimensionResource(R.dimen.slide_desc).value.sp,
	lineHeight: TextUnit = 16.sp,

	) {
	Text(
		text = text,
		modifier = modifier,
		textAlign = textAlign,
		fontSize = fontSize,
		lineHeight = lineHeight
	)
}

@SuppressLint("ModifierParameter")
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
		Modifier
			.size(dimensionResource(R.dimen.img_width_height))
			.then(modifier),
		contentScale = ContentScale.Fit,
		colorFilter = colorFilter
	)
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
			SlideDescription("Syncthing replaces proprietary cloud and data services with something open, trustworthy and decentralized.")
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

@Preview(uiMode = ThemeControls.UI_MODE)
@Composable
fun SlideComponentsFullPreview() {
	SyncthingandroidTheme(ThemeControls.PREVIEW_DARK_THEME, ThemeControls.isMonetEnabled) {
		Surface {
			Column(
				Modifier.padding(16.dp),
				Arrangement.Center,
				Alignment.CenterHorizontally
			) {
				SlideImage(painterResource(R.drawable.ic_launcher_monochrome))
				Spacer(Modifier.size(16.dp))
				SlideTitle("Introduction")
				SlideDescription("Syncthing replaces proprietary cloud and data services with something open, trustworthy and decentralized.")
			}
		}
	}
}
