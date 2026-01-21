package dev.benedek.syncthingandroid.ui.slides

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.ui.reusable.SlideImage
import dev.benedek.syncthingandroid.ui.reusable.SlideDescription
import dev.benedek.syncthingandroid.ui.reusable.SlideTitle
import dev.benedek.syncthingandroid.ui.theme.SyncthingandroidTheme
import dev.benedek.syncthingandroid.util.ThemeControls

// Reimplementation of the activity_firststart_slide_intro.xml
@Composable
fun IntroSlide() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = dimensionResource(R.dimen.dots_full_height)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        //SlideWelcomeTitle(stringResource(R.string.welcome_title)) // Legyen vagy ne?
        SlideImage(painterResource(id = R.drawable.ic_monochrome))
        Spacer(modifier = Modifier.height(16.dp))
        SlideTitle(stringResource(R.string.introduction))
        SlideDescription(stringResource(R.string.welcome_text))
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun IntroSlidePreview() {
    SyncthingandroidTheme(dynamicColor = ThemeControls.useDynamicColor) {
        IntroSlide()
    }
}