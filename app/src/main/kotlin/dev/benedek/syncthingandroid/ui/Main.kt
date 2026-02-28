package dev.benedek.syncthingandroid.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.Preview
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.ui.reusable.AppScaffold
import dev.benedek.syncthingandroid.ui.theme.SyncthingandroidTheme
import dev.benedek.syncthingandroid.util.ThemeControls


@Composable
fun Main() {
    AppScaffold(
        topAppBarTitle = stringResource(R.string.app_name),
        topNavigationIcon = Icons.Outlined.Menu,
        topNavigationOnClick = {/*TODO*/},
        floatingActionButton = {
            FloatingActionButton(onClick = {}) {
                Icon(Icons.Outlined.Add, contentDescription = "Add")
            }
        }
    ) {

    }
}


@Preview(showBackground = true, showSystemUi = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun MainPreview() {
    SyncthingandroidTheme(dynamicColor = ThemeControls.useDynamicColor) {
        Main()
    }
}