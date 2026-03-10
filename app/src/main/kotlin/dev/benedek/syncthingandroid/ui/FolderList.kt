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
fun FolderList() {
    // TODO: Implement
    Box(modifier = Modifier.fillMaxSize()) {
        Text("Folder List placeholder")
    }
}

@Composable
fun FolderListItem() {

}

@Preview
@Composable
fun FolderListPreview() {
    SyncthingandroidTheme(dynamicColor = ThemeControls.blurEnabled) {
        FolderList()
    }
}

@Preview
@Composable
fun FolderListItemPreview() {
    SyncthingandroidTheme(dynamicColor = ThemeControls.blurEnabled) {
        FolderListItem()
    }
}

