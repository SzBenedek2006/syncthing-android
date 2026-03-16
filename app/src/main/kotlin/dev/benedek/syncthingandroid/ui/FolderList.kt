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
import dev.benedek.syncthingandroid.model.Folder
import dev.benedek.syncthingandroid.ui.theme.SyncthingandroidTheme
import dev.benedek.syncthingandroid.util.ThemeControls

@Composable
fun FolderList(
    folders: List<Folder>
) {
    // TODO: Implement
    if (folders.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = stringResource(id = R.string.folder_list_empty))
        }
    }

}

@Composable
fun FolderListItem() {

}

@Preview
@Composable
fun FolderListPreview() {
    SyncthingandroidTheme(dynamicColor = ThemeControls.blurEnabled) {
        FolderList(emptyList())
    }
}

@Preview
@Composable
fun FolderListItemPreview() {
    SyncthingandroidTheme(dynamicColor = ThemeControls.blurEnabled) {
        FolderListItem()
    }
}

