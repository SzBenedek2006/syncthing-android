package dev.benedek.syncthingandroid.ui

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.activities.DeviceActivity
import dev.benedek.syncthingandroid.activities.FolderActivity
import dev.benedek.syncthingandroid.activities.SettingsActivity
import dev.benedek.syncthingandroid.activities.WebGuiActivity
import dev.benedek.syncthingandroid.service.SyncthingService
import dev.benedek.syncthingandroid.ui.reusable.AppScaffold
import dev.benedek.syncthingandroid.ui.reusable.CustomDialog
import dev.benedek.syncthingandroid.ui.reusable.HorizontalDivider
import dev.benedek.syncthingandroid.ui.reusable.OptionTile
import dev.benedek.syncthingandroid.ui.reusable.topBorderWithCorners
import dev.benedek.syncthingandroid.ui.theme.SyncthingandroidTheme
import dev.benedek.syncthingandroid.ui.theme.extendedColorScheme
import dev.benedek.syncthingandroid.util.ThemeControls
import dev.benedek.syncthingandroid.util.Util
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.tooling.preview.AndroidUiModes
import dev.benedek.syncthingandroid.util.ThemeControls.isBlurEnabled

@Composable
fun Main(viewModel: MainViewModel, exit: () -> Unit) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val contentColor = MaterialTheme.colorScheme.onSurface
    val red = MaterialTheme.extendedColorScheme.red.color
    val green = MaterialTheme.extendedColorScheme.green.color
    val blue = MaterialTheme.extendedColorScheme.blue.color
    val yellow = MaterialTheme.extendedColorScheme.yellow.color





    val density = LocalDensity.current
    val containerWidth = LocalWindowInfo.current.containerSize.width

    val pagerState = rememberPagerState(pageCount = { 2 })

    val drawerBlurAmount by remember {
        derivedStateOf {

            if (!isBlurEnabled) {
                0.dp
            } else if (drawerState.currentOffset.isNaN()) {
                // Fallback before the drawer's layout has been measured
                if (drawerState.currentValue == DrawerValue.Closed) ThemeControls.blurRadius.dp else 0.dp
            } else {
                // ModalDrawerSheet max width: 360.dp, or screen width if screen width < 360.dp
                val maxDrawerWidthPx = with(density) { 360.dp.toPx() }
                val screenWidthPx = with(density) { containerWidth.dp.toPx() }
                val actualDrawerWidthPx = minOf(maxDrawerWidthPx, screenWidthPx)


                val openFraction = 1f - (abs(drawerState.currentOffset) / actualDrawerWidthPx).coerceIn(0f, 1f)

                (openFraction * ThemeControls.blurRadius).dp
            }
        }
    }

    var predictiveBackProgress by remember { mutableFloatStateOf(0f) }

    PredictiveBackHandler(drawerState.isOpen) { progress: Flow<BackEventCompat> ->
        try {
            progress.collect { backEvent ->
                predictiveBackProgress = backEvent.progress
            }
            // This block is executed only if the gesture completes successfully.
            scope.launch {
                drawerState.close()
                animate(predictiveBackProgress, 0f) { value, _ ->
                    predictiveBackProgress = value
                }
            }
        } catch (_: CancellationException) {
            predictiveBackProgress = 0f
        } finally {

        }
    }


    val dialogBlurAmount by animateFloatAsState(
        targetValue = if (isBlurEnabled &&
            (viewModel.showDeviceIdDialog || viewModel.showExitDialog || viewModel.showRestartDialog)
            ) ThemeControls.blurRadius.toFloat() else 0f,
        label = "DialogBlurAnimation"
    )



    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            // DrawerDefaults.modalContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ModalDrawerSheet(
                windowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.End + WindowInsetsSides.Top
                ),
                modifier = Modifier
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .graphicsLayer {
                        // 1. Slide it away slightly
                        translationX = -predictiveBackProgress * 200f

                        // 2. Shrink it slightly (Modern Android look)
                        val scale = 1f - (predictiveBackProgress * 0.05f)
                        scaleX = scale
                        scaleY = scale

                        // 3. Smoothly round the corners more as it pulls away
                        shape = RoundedCornerShape(
                            topEnd = 16.dp + (predictiveBackProgress * 16).dp,
                            bottomEnd = 16.dp + (predictiveBackProgress * 16).dp
                        )
                        clip = true
                    }
            ) {
                Column(
                    Modifier
                        .windowInsetsPadding(WindowInsets.displayCutout)
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
                    Modifier
                        .windowInsetsPadding(WindowInsets.displayCutout)
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
                        Modifier
                            .windowInsetsPadding(WindowInsets.displayCutout)
                    ) {
                        OptionTile(
                            title = stringResource(R.string.show_device_id),
                            leftIconPainter = painterResource(R.drawable.ic_qrcode_24dp),
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                }
                                viewModel.showDeviceIdDialog = true
                            }
                        )
                        OptionTile(
                            title = stringResource(R.string.web_gui_title),
                            leftIconPainter = painterResource(R.drawable.ic_web_24dp),
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                }
                                context.startActivity(Intent(context, WebGuiActivity::class.java))
                            }
                        )
                        OptionTile(
                            title = stringResource(R.string.settings_title),
                            leftIconPainter = painterResource(R.drawable.ic_settings_24dp),
                            onClick = {
                                scope.launch {
                                    drawerState.close()
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
                                    drawerState.close()
                                }
                                viewModel.showRestartDialog = true
                            }
                        )
                        OptionTile(
                            title = stringResource(R.string.exit),
                            leftIconPainter = rememberVectorPainter(Icons.Outlined.PowerSettingsNew),
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                }
                                viewModel.showExitDialog = true
                            }
                        )
                    }
                }

            }
        }
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .blur(max(drawerBlurAmount, dialogBlurAmount.dp))
        ) {
            AppScaffold(
                topAppBarTitle = stringResource(R.string.app_name),
                topNavigationIcon = Icons.Outlined.Menu,
                topNavigationOnClick = {
                    scope.launch {
                        drawerState.apply {
                            if (isClosed) open() else close()
                        }
                    }
                },
                floatingActionButton = {
                    FloatingActionButton(onClick = {
                        when (pagerState.currentPage) {
                            0 -> {
                                val intent = Intent(context, FolderActivity::class.java)
                                    .putExtra(FolderViewModel.EXTRA_IS_CREATE, true)
                                context.startActivity(intent)
                            }
                            1 -> {
                                val intent = Intent(context, DeviceActivity::class.java)
                                    .putExtra(DeviceActivity.EXTRA_IS_CREATE, true)
                                context.startActivity(intent)
                            }
                            else -> {
                                Toast.makeText(context, "Invalid page, this should never happen!", Toast.LENGTH_SHORT)
                                    .show()
                                Log.wtf("FAB onClick", "Invalid page, this should never happen!")
                            }
                        }

                    }) {
                        Icon(Icons.Outlined.Add, contentDescription = "Add")
                    }
                },
                bottomBar = {
                    NavigationBar() {
                        NavigationBarItem(
                            selected = pagerState.currentPage == 0,
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(0)
                                }
                            },
                            icon = { Icon(Icons.Outlined.Folder, stringResource(R.string.folders_fragment_title)) },
                            label = { Text(stringResource(R.string.folders_fragment_title)) }
                        )
                        NavigationBarItem(
                            selected = pagerState.currentPage == 1,
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(1)
                                }
                            },
                            icon = { Icon(Icons.Outlined.Devices, stringResource(R.string.devices_fragment_title)) },
                            label = { Text(stringResource(R.string.devices_fragment_title)) }
                        )
                    }
                }
            ) { paddingValues ->

                val folderStatusesMap by viewModel.folderStatuses.collectAsStateWithLifecycle()

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) { page ->
                    when (page) {
                        0 -> FolderList(viewModel.folders, folderStatusesMap, viewModel.isApiReady)
                        1 -> DeviceList(viewModel.devices ?: emptyList(), viewModel.deviceStatuses, viewModel.isApiReady)
                    }
                }

                // Dialogs
                if (viewModel.showDeviceIdDialog) {
                    if (viewModel.systemInfo?.myID != null) {
                        QrCodeDialog(
                            viewModel.systemInfo!!.myID!!,
                            { viewModel.showDeviceIdDialog = false },
                            remember { viewModel.generateQrBitmap(viewModel.systemInfo!!.myID)!! }
                        )
                    } else {
                        viewModel.showDeviceIdDialog = false
                        Toast.makeText(context, R.string.could_not_access_deviceid, Toast.LENGTH_SHORT)
                            .show()
                    }
                }

                if (viewModel.showRestartDialog)
                    RestartDialog(viewModel, context)
                if (viewModel.showExitDialog)
                    ExitDialog(viewModel, context, exit)


            }
        }
    }
}


// DIALOGS

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun QrCodeDialog(
    deviceId: String,
    onDismissRequest: () -> Unit,
    qrCode: Bitmap
) {
    CustomDialog(
        stringResource(R.string.device_id),
        null,
        onDismissRequest,
        null,
        "",
        stringResource(R.string.finish)
    ) {
        val clipboard = LocalClipboard.current
        val scope = rememberCoroutineScope()
        val context = LocalContext.current

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth()) {
                Text(
                    deviceId,
                    Modifier
                        .weight(1f)
                        .padding(vertical = 6.dp),
                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 15.sp,
                    letterSpacing = 0.25.sp
                )
                IconButton(
                    onClick = {
                        val clipData = ClipData.newPlainText(
                            context.getString(R.string.device_id),
                            deviceId
                        )
                        val clipEntry = ClipEntry(clipData)
                        scope.launch {
                            clipboard.setClipEntry(clipEntry)
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_content_copy_24dp),
                        contentDescription =  stringResource(android.R.string.copy)
                    )
                }
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, deviceId)
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, null)
                IconButton(
                    onClick = { context.startActivity(shareIntent) }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_share_24dp),
                        contentDescription =  stringResource(R.string.share_title)
                    )
                }
            }

            Image(
                qrCode.asImageBitmap(),
                null,
                Modifier
                    .padding(vertical = 10.dp)
                    .fillMaxWidth(0.8f),
                contentScale = ContentScale.FillWidth
            )
        }
    }
}

@Composable
fun RestartDialog(viewModel: MainViewModel, context: Context) {
    val onDismissRequest = { viewModel.showRestartDialog = false }
    AlertDialog(
        confirmButton = {
            TextButton(
                onClick = {
                    val intent = Intent(context, SyncthingService::class.java).apply {
                        action = SyncthingService.ACTION_RESTART
                    }
                    context.startService(intent)
                    onDismissRequest()
                }
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest
            ) {
                Text(stringResource(android.R.string.cancel))
            }
        },
        title = {
            Text(
                stringResource(R.string.restart_question),
                fontSize = MaterialTheme.typography.titleLarge.fontSize
            )
        },
        text = {
            Text(
                stringResource(R.string.restart_description),
                fontSize = MaterialTheme.typography.labelLarge.fontSize
            )
        },
        onDismissRequest = onDismissRequest,
        modifier = Modifier,
    )
}

@Composable
fun ExitDialog(viewModel: MainViewModel, context: Context, exit: () -> Unit) {
    val onDismissRequest = { viewModel.showExitDialog = false }
    AlertDialog(
        confirmButton = {
            TextButton(
                onClick = exit
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest
            ) {
                Text(stringResource(android.R.string.cancel))

            }
        },
        title = {
            Text(
                stringResource(R.string.exit_question),
                fontSize = MaterialTheme.typography.titleLarge.fontSize
            )
        },
        text = {
            Text(
                stringResource(R.string.exit_description),
                fontSize = MaterialTheme.typography.labelLarge.fontSize
            )
        },
        onDismissRequest = onDismissRequest,
        modifier = Modifier,
    )}




// PREVIEWS

@Preview
@Composable
fun MainPreview() {
    SyncthingandroidTheme(dynamicColor = ThemeControls.isMonetEnabled) {
        Main(viewModel<MainViewModel>(), {})
    }
}

@Preview(showBackground = true, uiMode = ThemeControls.UI_MODE)
@Composable
fun QrCodeDialogPreview() {
    SyncthingandroidTheme(darkTheme = ThemeControls.useDarkMode, dynamicColor = ThemeControls.isMonetEnabled) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            QrCodeDialog(
                "SAKD75B-GZGOLNW-G5MFIJU-24GFFJG-SES7W7L-KQKKSFJ-TUT4FVA-GTZMDAE",
                {  },
                remember { MainViewModel().generateQrBitmap("SAKD75B-GZGOLNW-G5MFIJU-24GFFJG-SES7W7L-KQKKSFJ-TUT4FVA-GTZMDAE")!! }
            )
        }
    }
}

@Preview(showBackground = true, uiMode = ThemeControls.UI_MODE)
@Composable
fun RestartDialogPreview() {
    SyncthingandroidTheme(darkTheme = ThemeControls.useDarkMode, dynamicColor = ThemeControls.isMonetEnabled) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            RestartDialog(viewModel(), LocalContext.current)
        }
    }
}

@Preview(showBackground = true, uiMode = ThemeControls.UI_MODE)
@Composable
fun ExitDialogPreview() {
    SyncthingandroidTheme(darkTheme = ThemeControls.useDarkMode, dynamicColor = ThemeControls.isMonetEnabled) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            ExitDialog(viewModel(), LocalContext.current, {})
        }
    }
}