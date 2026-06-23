package dev.benedek.syncthingandroid.ui.slides

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.ui.LocalIsLandscape
import dev.benedek.syncthingandroid.ui.reusable.AdaptiveSlideLayout
import dev.benedek.syncthingandroid.ui.reusable.SlideDescription
import dev.benedek.syncthingandroid.ui.reusable.SlideImage
import dev.benedek.syncthingandroid.ui.reusable.SlideTitle
import dev.benedek.syncthingandroid.ui.reusable.TextLayout
import dev.benedek.syncthingandroid.ui.theme.SyncthingandroidTheme
import dev.benedek.syncthingandroid.util.ThemeControls

// Reimplementation of the activity_firststart_slide_intro.xml
@Composable
fun IntroSlide() {
	AdaptiveSlideLayout(
		{
			SlideTitle(stringResource(R.string.introduction))
		},
		{
			SlideDescription(
				AnnotatedString(stringResource(R.string.welcome_subtitle)),
				AnnotatedString.fromHtml(
					stringResource(R.string.welcome_text),
					TextLinkStyles(SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline))
				),
				textLayout = TextLayout.Expandable
			)
		},
		Modifier,
		{
			SlideImage(painterResource(R.drawable.ic_monochrome))
		}
	)
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun IntroSlidePreview() {
	SyncthingandroidTheme(dynamicColor = ThemeControls.isMonetEnabled) {
		Surface {
			IntroSlide()
		}
	}
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 800, heightDp = 400)
@Composable
fun IntroSlideLandscapePreview() {
	SyncthingandroidTheme(dynamicColor = ThemeControls.isMonetEnabled) {
		CompositionLocalProvider(LocalIsLandscape provides true) {
			Surface {
				IntroSlide()
			}
		}
	}
}
