package dev.benedek.syncthingandroid.ui.slides

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.ui.reusable.SlideDescription
import dev.benedek.syncthingandroid.ui.reusable.SlideTitle
import dev.benedek.syncthingandroid.ui.theme.SyncthingandroidTheme
import dev.benedek.syncthingandroid.util.ThemeControls

// Reimplementation of the activity_firststart_slide_api_level_30.xml

@Composable
fun ApiUpgradeSlide(
    onButtonClick: () -> Unit,
    isApiUpgraded: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = dimensionResource(R.dimen.dots_full_height)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center

    ) {
        val context = LocalContext.current

        SlideTitle(stringResource(R.string.api_level_30_title))
        SlideDescription(stringResource(R.string.api_level_30_desc))
        Button(
            onClick = onButtonClick,
            enabled = !isApiUpgraded
        ) {
            Text(
                if (!isApiUpgraded) {
                    stringResource(R.string.api_level_30_button)
                } else {
                    stringResource(R.string.api_level_30_button)
                }
            )
        }
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ApiUpgradeSlidePreview() {
    SyncthingandroidTheme(dynamicColor = ThemeControls.getUseDynamicColor()) {
        ApiUpgradeSlide({}, true)
    }
}