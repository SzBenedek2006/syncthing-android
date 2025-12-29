package dev.benedek.syncthingandroid.ui

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.AndroidUiModes
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.ui.reusable.ImageSlide
import dev.benedek.syncthingandroid.ui.reusable.SlideDescription
import dev.benedek.syncthingandroid.ui.reusable.SlideTitle
import dev.benedek.syncthingandroid.ui.reusable.SlideWelcomeTitle
import dev.benedek.syncthingandroid.ui.theme.SyncthingandroidTheme

@Composable
fun LocationSlide(
    onButtonClick: () -> Unit,
    isButtonActivated: @Composable (Context) -> Boolean
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

        //SlideWelcomeTitle(stringResource(R.string.welcome_title))
        ImageSlide(painterResource(R.drawable.ic_location))
        Spacer(modifier = Modifier.height(16.dp))
        SlideTitle(stringResource(R.string.location_permission_title))
        SlideDescription(stringResource(R.string.location_permission_desc))
        Button(
            onClick = onButtonClick,
            // Hacky solution, but seems to work.
            enabled = isButtonActivated(context)
        ) {
            Text(
                if (isButtonActivated(context)) {
                    stringResource(R.string.grant_permission)
                } else {
                    stringResource(R.string.permission_granted)
                }
            )
        }
    }
}

@Preview(showBackground = true, uiMode = AndroidUiModes.UI_MODE_NIGHT_YES)
@Composable
fun LocationSlidePreview() {
    SyncthingandroidTheme {
        LocationSlide({}, {true})
    }
}