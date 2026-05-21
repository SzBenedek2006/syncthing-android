package dev.benedek.syncthingandroid.ui.main

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Web
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.activities.SettingsActivity
import dev.benedek.syncthingandroid.activities.WebGuiActivity
import dev.benedek.syncthingandroid.ui.MainViewModel
import dev.benedek.syncthingandroid.ui.reusable.HorizontalDivider
import dev.benedek.syncthingandroid.ui.reusable.OptionTile
import dev.benedek.syncthingandroid.ui.reusable.topBorderWithCorners
import dev.benedek.syncthingandroid.ui.theme.SyncthingandroidTheme
import dev.benedek.syncthingandroid.ui.theme.extendedColorScheme
import dev.benedek.syncthingandroid.util.ThemeControls
import dev.benedek.syncthingandroid.util.Util
import kotlinx.coroutines.launch

@Composable
fun MainModalDrawerSheet(
    predictiveBackProgress: () -> Float,
    drawerState: () -> DrawerState,
    viewModel: MainViewModel
    ) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val contentColor = MaterialTheme.colorScheme.onSurface
    val red = MaterialTheme.extendedColorScheme.red.color
    val green = MaterialTheme.extendedColorScheme.green.color
    val blue = MaterialTheme.extendedColorScheme.blue.color
    val yellow = MaterialTheme.extendedColorScheme.yellow.color

    ModalDrawerSheet(
        windowInsets = WindowInsets(),
        modifier = Modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .graphicsLayer {
                // 1. Slide it away slightly
                translationX = -predictiveBackProgress() * 200f

                // 2. Shrink it slightly (Modern Android look)
                val scale = 1f - (predictiveBackProgress() * 0.05f)
                scaleX = scale
                scaleY = scale

                // 3. Smoothly round the corners more as it pulls away
                shape = RoundedCornerShape(
                    topEnd = 16.dp + (predictiveBackProgress() * 16).dp,
                    bottomEnd = 16.dp + (predictiveBackProgress() * 16).dp
                )
                clip = true
            }
    ) {
        Column(
            Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Start
                ))
        ) {
            Text(
                stringResource(R.string.app_name),
                fontSize = MaterialTheme.typography.titleLarge.fontSize,
                fontStyle = MaterialTheme.typography.titleLarge.fontStyle,
                fontWeight = MaterialTheme.typography.titleLarge.fontWeight,
                fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                letterSpacing = MaterialTheme.typography.titleLarge.letterSpacing,
                modifier = Modifier
                    .padding(16.dp)
            )
        }

        HorizontalDivider()


        Column(
            Modifier.windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Start)
            )
        ) {
            OptionTile(
                title = stringResource(R.string.ram_usage),
                description = Util.readableFileSize(context, viewModel.systemInfo?.sys ?: 0),
                noIconPadding = true,
                contentColor = contentColor,
                enabled = viewModel.api != null
            )
            OptionTile(
                title = stringResource(R.string.download_title),
                description = Util.readableTransferRate(context, viewModel.deviceStatuses.total?.inBits ?: 0),
                noIconPadding = true,
                contentColor = contentColor,
                enabled = viewModel.api != null
            )
            OptionTile(
                title = stringResource(R.string.upload_title),
                description = Util.readableTransferRate(context, viewModel.deviceStatuses.total?.outBits ?: 0),
                noIconPadding = true,
                contentColor = contentColor,
                enabled = viewModel.api != null
            )
            OptionTile(
                title = stringResource(R.string.announce_server),
                description = "${viewModel.announceConnected}/${viewModel.announceTotal}",
                descriptionColor = if (viewModel.announceConnected > 0) green else red,
                noIconPadding = true,
                contentColor = contentColor,
                enabled = viewModel.api != null
            )
            OptionTile(
                title = stringResource(R.string.syncthing_version_title),
                description = viewModel.systemVersion?.version ?: "null",
                noIconPadding = true,
                contentColor = contentColor,
                enabled = viewModel.api != null
            )
        }


        HorizontalDivider()
        Spacer(Modifier.weight(1f))


        Surface(
            shape = RoundedCornerShape(0.dp, 16.dp, 16.dp, 0.dp),
            color = Color.Transparent,
            modifier = Modifier.topBorderWithCorners(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant,
                16.dp
            )
        ) {
            Column(
                Modifier.windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Start)
                )
            ) {
                OptionTile(
                    title = stringResource(R.string.web_gui_title),
                    leftIconPainter = rememberVectorPainter(Icons.Outlined.Web),
                    onClick = {
                        scope.launch {
                            drawerState().close()
                        }
                        context.startActivity(Intent(context, WebGuiActivity::class.java))
                    }
                )
                OptionTile(
                    title = stringResource(R.string.show_device_id),
                    leftIconPainter = painterResource(R.drawable.ic_qrcode_24dp),
                    onClick = {
                        scope.launch {
                            drawerState().close()
                        }
                        viewModel.showDeviceIdDialog = true
                    }
                )
                OptionTile(
                    title = stringResource(R.string.settings_title),
                    leftIconPainter = rememberVectorPainter(Icons.Outlined.Settings),
                    onClick = {
                        scope.launch {
                            drawerState().close()
                        }
                        context.startActivity(Intent(context, SettingsActivity::class.java))
                    }
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(0.dp, 16.dp, 16.dp, 0.dp),
        ) {
            Column(
                Modifier
                    .navigationBarsPadding()
                    .windowInsetsPadding(WindowInsets.displayCutout)
            ) {
                OptionTile(
                    title = stringResource(R.string.restart),
                    leftIconPainter = painterResource(R.drawable.ic_autorenew_24dp),
                    onClick = {
                        scope.launch {
                            drawerState().close()
                        }
                        viewModel.showRestartDialog = true
                    }
                )
                OptionTile(
                    title = stringResource(R.string.exit),
                    leftIconPainter = rememberVectorPainter(Icons.Outlined.PowerSettingsNew),
                    onClick = {
                        scope.launch {
                            drawerState().close()
                        }
                        viewModel.showExitDialog = true
                    }
                )
            }
        }

    }
}



@Preview(uiMode = ThemeControls.UI_MODE)
@Composable
fun MainModalDrawerSheetPreview() {
    SyncthingandroidTheme(ThemeControls.useDarkMode, ThemeControls.isMonetEnabled) {
        MainModalDrawerSheet({ 0f }, { DrawerState(DrawerValue.Open) }, viewModel<MainViewModel>())
    }
}