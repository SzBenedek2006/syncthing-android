package dev.benedek.syncthingandroid.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.benedek.syncthingandroid.ui.theme.SyncthingandroidTheme
import dev.benedek.syncthingandroid.util.ThemeControls

@Composable
fun DeviceList() {
    // TODO: Implement
    Box(modifier = Modifier.fillMaxSize()) {
        Text("Device List placeholder")
    }
}


@Composable
fun DeviceListItem() {

}

@Preview
@Composable
fun DeviceListPreview() {
    SyncthingandroidTheme(dynamicColor = ThemeControls.blurEnabled) {
        DeviceList()
    }
}

@Preview
@Composable
fun DeviceListItemPreview() {
    SyncthingandroidTheme(dynamicColor = ThemeControls.blurEnabled) {
        DeviceListItem()
    }
}