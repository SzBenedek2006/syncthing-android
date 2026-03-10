package dev.benedek.syncthingandroid.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.activities.SettingsActivity
import dev.benedek.syncthingandroid.activities.WebGuiActivity
import dev.benedek.syncthingandroid.service.SyncthingService
import dev.benedek.syncthingandroid.ui.reusable.AppScaffold
import dev.benedek.syncthingandroid.ui.reusable.CustomDialog
import dev.benedek.syncthingandroid.ui.reusable.HorizontalDivider
import dev.benedek.syncthingandroid.ui.reusable.OptionTile
import dev.benedek.syncthingandroid.ui.reusable.topBorderWithCorners
import dev.benedek.syncthingandroid.ui.theme.SyncthingandroidTheme
import dev.benedek.syncthingandroid.util.ThemeControls
import dev.benedek.syncthingandroid.util.Util
import kotlinx.coroutines.launch
import kotlin.math.abs


@Composable
fun Main(viewModel: MainViewModel, exit: () -> Unit) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Open)
    val scope = rememberCoroutineScope()
    val contentColor = MaterialTheme.colorScheme.onSurface

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "folders"

    val density = LocalDensity.current
    val containerWidth = LocalWindowInfo.current.containerSize.width

    val drawerBlurAmount by remember {
        derivedStateOf {

            if (!ThemeControls.blurEnabled) {
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
    val dialogBlurAmount by animateFloatAsState(
        targetValue = if (ThemeControls.blurEnabled &&
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
                        description = Util.readableTransferRate(context, viewModel.connections?.total?.inBits ?: 0),
                        noIconPadding = true,
                        contentColor = contentColor,
                        enabled = viewModel.api != null
                    )
                    OptionTile(
                        title = stringResource(R.string.upload_title),
                        description = Util.readableTransferRate(context, viewModel.connections?.total?.outBits ?: 0),
                        noIconPadding = true,
                        contentColor = contentColor,
                        enabled = viewModel.api != null
                    )
                    OptionTile(
                        title = stringResource(R.string.announce_server),
                        description = "${viewModel.announceConnected}/${viewModel.announceTotal}",
                        descriptionColor = if (viewModel.announceConnected > 0) Color.Green else Color.Red,
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
                    FloatingActionButton(onClick = {/*TODO*/}) {
                        Icon(Icons.Outlined.Add, contentDescription = "Add")
                    }
                },
                bottomBar = {
                    NavigationBar() {
                        NavigationBarItem(
                            selected = currentRoute == "folders",
                            onClick = {
                                navController.navigate("folders") {
                                // Pop up to start destination so back stack doesn't build up
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                                      },
                            icon = { Icon(Icons.Outlined.Folder, stringResource(R.string.folders_fragment_title)) },
                            label = { Text(stringResource(R.string.folders_fragment_title)) }
                        )
                        NavigationBarItem(
                            selected = currentRoute == "devices",
                            onClick = {
                                navController.navigate("devices") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                                      },
                            icon = { Icon(Icons.Outlined.Devices, stringResource(R.string.devices_fragment_title)) },
                            label = { Text(stringResource(R.string.devices_fragment_title)) }
                        )
                    }
                }
            ) { paddingValues ->

                NavHost(
                    navController = navController,
                    startDestination = "folders",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    enterTransition = {
                        val initialIndex = if (initialState.destination.route == "folders") 0 else 1
                        val targetIndex = if (targetState.destination.route == "folders") 0 else 1

                        if (targetIndex > initialIndex) {
                            slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }) + fadeIn()
                        } else {
                            slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth }) + fadeIn()
                        }
                    },
                    exitTransition = {
                        val initialIndex = if (initialState.destination.route == "folders") 0 else 1
                        val targetIndex = if (targetState.destination.route == "folders") 0 else 1

                        // Slide out to left if moving forward, to right if moving backward (or back button)
                        if (targetIndex > initialIndex) {
                            slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth }) + fadeOut()
                        } else {
                            slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }) + fadeOut()
                        }
                    }
                ) {
                    composable("folders") { FolderList() }
                    composable("devices") { DeviceList() }
                }

                // Dialogs
                if (viewModel.showDeviceIdDialog) {
                    if (viewModel.systemInfo?.myID != null) {
                        QrCodeDialog(viewModel.systemInfo!!.myID!!, viewModel)
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

@Composable
fun QrCodeDialog(deviceId: String, viewModel: MainViewModel) {
    CustomDialog(
        stringResource(R.string.device_id),
        null,
        { viewModel.showDeviceIdDialog = false },
        null,
        "",
        stringResource(R.string.finish)
    ) {
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
                    onClick = { /*TODO*/ }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_content_copy_24dp),
                        contentDescription =  stringResource(android.R.string.copy)
                    )
                }
                IconButton(
                    onClick = { /*TODO*/ }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_share_24dp),
                        contentDescription =  stringResource(R.string.share_title)
                    )
                }
            }

            val qrCode = remember { viewModel.generateQrBitmap(deviceId) }
            if (qrCode != null)
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
                stringResource(R.string.restart),
                fontSize = MaterialTheme.typography.titleLarge.fontSize
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
                stringResource(R.string.exit),
                fontSize = MaterialTheme.typography.titleLarge.fontSize
            )
        },
        onDismissRequest = onDismissRequest,
        modifier = Modifier,
    )}




// PREVIEWS

@Preview
@Composable
fun MainPreview() {
    SyncthingandroidTheme(dynamicColor = ThemeControls.useDynamicColor) {
        Main(viewModel<MainViewModel>(), {})
    }
}

@Preview
@Composable
fun QrCodeDialogPreview() {
    SyncthingandroidTheme() {
        QrCodeDialog("SAKD75B-GZGOLNW-G5MFIJU-24GFFJG-SES7W7L-KQKKSFJ-TUT4FVA-GTZMDAE", viewModel())
    }

}

@Preview
@Composable
fun RestartDialogPreview() {
    SyncthingandroidTheme(dynamicColor = ThemeControls.useDynamicColor) {
        RestartDialog(viewModel(), LocalContext.current)
    }
}

@Preview
@Composable
fun ExitDialogPreview() {
    SyncthingandroidTheme(dynamicColor = ThemeControls.useDynamicColor) {
        ExitDialog(viewModel(), LocalContext.current, {})
    }
}