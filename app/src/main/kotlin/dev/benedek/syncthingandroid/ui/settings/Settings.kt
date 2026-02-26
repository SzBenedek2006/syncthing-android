package dev.benedek.syncthingandroid.ui.settings

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Rule
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Rule
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.SettingsApplications
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.material.icons.outlined.ToggleOn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.AndroidUiModes
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.preference.PreferenceManager
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.service.Constants
import dev.benedek.syncthingandroid.ui.reusable.OptionTile
import dev.benedek.syncthingandroid.ui.reusable.preventClicksWhenExiting
import dev.benedek.syncthingandroid.ui.settings.categories.About
import dev.benedek.syncthingandroid.ui.settings.categories.Backup
import dev.benedek.syncthingandroid.ui.settings.categories.Behaviour
import dev.benedek.syncthingandroid.ui.settings.categories.Debug
import dev.benedek.syncthingandroid.ui.settings.categories.Experimental
import dev.benedek.syncthingandroid.ui.settings.categories.RunConditions
import dev.benedek.syncthingandroid.ui.settings.categories.SyncthingOptions
import dev.benedek.syncthingandroid.ui.settings.categories.Theme
import dev.benedek.syncthingandroid.ui.theme.SyncthingandroidTheme
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.preference

// TODO: Use Constants for keys everywhere

@Composable
fun Settings(
    viewModel: SettingsViewModel,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    navController: NavHostController
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadInitialValues(context)
    }

    LaunchedEffect(viewModel) {


        // --- TEXT FIELDS ---
        launch {
            snapshotFlow { viewModel.deviceName.value }
                .drop(1)
                .collect { viewModel.updateDeviceName(it) }
        }
        launch {
            snapshotFlow { viewModel.listenAddresses.value }
                .drop(1)
                .collect { viewModel.updateSettings(newListenAddresses = it) }
        }
        launch {
            snapshotFlow { viewModel.maxRecvKbps.value }
                .drop(1)
                .collect { viewModel.updateSettings(newMaxRecv = it) }
        }
        launch {
            snapshotFlow { viewModel.maxSendKbps.value }
                .drop(1)
                .collect { viewModel.updateSettings(newMaxSend = it) }
        }
        launch {
            snapshotFlow { viewModel.globalAnnounceServers.value }
                .drop(1)
                .collect { viewModel.updateSettings(newAnnounceServers = it) }
        }
        launch {
            snapshotFlow { viewModel.guiAddress.value }
                .drop(1)
                .collect { viewModel.updateSettings(newGuiAddress = it) }
        }
        launch {
            snapshotFlow { viewModel.environmentVariables.value }
                .drop(1)
                .collect { viewModel.updateEnvironmentVariables(context) }
        }
        launch {
            snapshotFlow { viewModel.httpProxyAddress.value }
                .drop(1)
                .collect { viewModel.updateHttpProxy(context) }
        }
        launch {
            snapshotFlow { viewModel.socksProxyAddress.value }
                .drop(1)
                .collect { viewModel.updateSocksProxy(context) }
        }

        // --- SWITCHES ---
        launch {
            snapshotFlow { viewModel.natEnabled.value }
                .drop(1)
                .collect { viewModel.updateSettings(newNatEnabled = it) }
        }
        launch {
            snapshotFlow { viewModel.localAnnounceEnabled.value }
                .drop(1)
                .collect { viewModel.updateSettings(newLocalAnnounceEnabled = it) }
        }
        launch {
            snapshotFlow { viewModel.globalAnnounceEnabled.value }
                .drop(1)
                .collect { viewModel.updateSettings(newGlobalAnnounceEnabled = it) }
        }
        launch {
            snapshotFlow { viewModel.relaysEnabled.value }
                .drop(1)
                .collect { viewModel.updateSettings(newRelaysEnabled = it) }
        }
        launch {
            snapshotFlow { viewModel.urAccepted.value }
                .drop(1)
                .collect { viewModel.updateSettings(newUrAccepted = it) }
        }
        launch {
            snapshotFlow { viewModel.useRoot.value }
                .drop(1)
                .collect { viewModel.onRootChanged(context, viewModel.useRoot.value) }
        }
        launch {
            snapshotFlow { viewModel.useTor.value }
                .drop(1)
                .collect { viewModel.restartSyncthing() }
        }

    }



    ProvidePreferenceLocals {
        NavHost(
            navController = navController,
            startDestination = "settings_root",
            modifier = Modifier.fillMaxSize(),
            enterTransition = {
                slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }) + fadeIn()
            },
            // 2. Open new screen: Old screen slides left VERY slightly (parallax) and fades
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth / 4 }) + fadeOut()
            },

            // 3. Pressing Back: Old screen comes back from the slight left
            popEnterTransition = {
                slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth / 4 }) + fadeIn()
            },
            // 4. Pressing Back: Current screen slides fully out to the Right
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }) + fadeOut()
            }
        ) {
            composable("settings_root") {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .preventClicksWhenExiting()
                        .padding(contentPadding)
                ) {
                    OptionTile(
                        title = stringResource(R.string.preference_theme_title),
                        onClick = { navController.navigate("theme") },
                        leftIconPainter = rememberVectorPainter(Icons.Outlined.Palette)
                    )
                    OptionTile(
                        title = stringResource(R.string.category_run_conditions),
                        onClick = { navController.navigate("run_conditions") },
                        leftIconPainter = rememberVectorPainter(Icons.AutoMirrored.Outlined.Rule)
                    )
                    OptionTile(
                        title = stringResource(R.string.category_behaviour),
                        onClick = { navController.navigate("behaviour") },
                        leftIconPainter = rememberVectorPainter(Icons.Outlined.ToggleOn)
                    )
                    OptionTile(
                        title = stringResource(R.string.category_syncthing_options),
                        onClick = { navController.navigate("syncthing_options") },
                        leftIconPainter = painterResource(R.drawable.ic_stat_notify)
                    )
                    OptionTile(
                        title = stringResource(R.string.category_backup),
                        onClick = { navController.navigate("backup") },
                        leftIconPainter = rememberVectorPainter(Icons.Outlined.SettingsBackupRestore)
                    )
                    OptionTile(
                        title = stringResource(R.string.category_debug),
                        onClick = { navController.navigate("debug") },
                        leftIconPainter = rememberVectorPainter(Icons.Outlined.BugReport)
                    )
                    OptionTile(
                        title = stringResource(R.string.category_experimental),
                        onClick = { navController.navigate("experimental") },
                        leftIconPainter = rememberVectorPainter(Icons.Outlined.Science)
                    )
                    OptionTile(
                        title = stringResource(R.string.category_about),
                        onClick = { navController.navigate("about") },
                        leftIconPainter = rememberVectorPainter(Icons.Outlined.Info)
                    )
                }
            }

            composable("theme") { Theme(contentPadding) }
            composable("run_conditions") { RunConditions(contentPadding) }
            composable("behaviour") { Behaviour(contentPadding) }
            composable("syncthing_options") { SyncthingOptions(contentPadding, viewModel) }
            composable("backup") { Backup(contentPadding, viewModel) }
            composable("debug") { Debug(contentPadding, viewModel) }
            composable("experimental") { Experimental(contentPadding, viewModel) }
            composable("about") { About(contentPadding, viewModel) }
        }
    }
}


@Composable
@Preview(showBackground = true, uiMode = AndroidUiModes.UI_MODE_NIGHT_YES)
fun SettingsPreview() {
    SyncthingandroidTheme {
        Settings(viewModel<SettingsViewModel>(), navController = rememberNavController())
    }
}