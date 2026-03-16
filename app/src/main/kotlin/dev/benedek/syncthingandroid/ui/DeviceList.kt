package dev.benedek.syncthingandroid.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.model.Device
import dev.benedek.syncthingandroid.ui.theme.SyncthingandroidTheme
import dev.benedek.syncthingandroid.util.ThemeControls

@Composable
fun DeviceList(
    devices: List<Device>
) {
    // TODO: Implement
    if (devices.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = stringResource(id = R.string.devices_list_empty))
        }
    }
}


@Composable
fun DeviceListItem() {

}

@Preview
@Composable
fun DeviceListPreview() {
    SyncthingandroidTheme(dynamicColor = ThemeControls.blurEnabled) {
        DeviceList(emptyList())
    }
}

@Preview
@Composable
fun DeviceListItemPreview() {
    SyncthingandroidTheme(dynamicColor = ThemeControls.blurEnabled) {
        DeviceListItem()
    }
}