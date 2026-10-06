package dev.benedek.syncthingandroid.ui

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import dev.benedek.syncthingandroid.ui.reusable.AppScaffold
import dev.benedek.syncthingandroid.ui.theme.SyncthingandroidTheme
import dev.benedek.syncthingandroid.util.ThemeControls
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.model.Folder
import dev.benedek.syncthingandroid.ui.reusable.ComposeDialog
import dev.benedek.syncthingandroid.viewmodel.ShareViewModel

/**
 * Share screen.
 * First draft was made by an LLM, based on activity_share.xml
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareScreen(
    // State
    files: MutableMap<Uri, String>,
    folders: List<Folder>,
    selectedFolder: Int,
    subDirectory: String,
    isMultipleFiles: Boolean,
    showProgressDialog: Boolean,
    copyResult: ShareViewModel.Companion.CopyResult?,
    // Events
    onFolderSelect: (Int) -> Unit,
    onFileRemove: (Uri) -> Unit,
    onBrowseClick: () -> Unit,
    onSaveClick: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    LaunchedEffect(copyResult) {

        if (copyResult != null) {
            val label = folders[selectedFolder].label ?: folders[selectedFolder].path ?: ""

            val string: String = if (copyResult.hasError) resources.getString(R.string.copy_exception)
                else if (copyResult.ignored > 0) resources.getQuantityString(
                R.plurals.copy_success_partially,
                copyResult.copied,
                copyResult.copied, label, copyResult.ignored
            ) else {
                resources.getQuantityString(
                    R.plurals.copy_success,
                    copyResult.copied,
                    copyResult.copied,
                    label
                )
            }
            Toast.makeText(
                context,
                string,
                Toast.LENGTH_LONG
            ).show()
            onFinish()
        }
    }
    AppScaffold(
        modifier = modifier.fillMaxSize(),
        topAppBarTitle = stringResource(R.string.share_activity_title)
    ) { paddingValues ->

        // Replaces ScrollView + LinearLayout + ConstraintLayout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // File Name Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isMultipleFiles) stringResource(R.string.files_list) else stringResource(R.string.file_name),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(files.entries.toList()) { file ->
                        FileItem(file.value) { onFileRemove(file.key) }
                    }
                }

            }

            // Folders Spinner Section
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Folder",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (folders.isNotEmpty()) {
                    var expanded by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        val folder = folders[selectedFolder]
                        OutlinedTextField(
                            value = folder.label ?: folder.path ?: "",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            folders.forEachIndexed { index, folder ->
                                DropdownMenuItem(
                                    text = { Text(folder.label ?: folder.path ?: "") },
                                    onClick = {
                                        onFolderSelect(index)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Text(stringResource(R.string.folder_list_empty))
                }
            }

            // Sub-directory Section
            if (folders.isNotEmpty())
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Sub folder",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = subDirectory.ifEmpty { "No sub folder is selected" },
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (subDirectory.isEmpty())
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Button(onClick = onBrowseClick) {
                            Text("Browse")
                        }
                    }
                }

            // Pushes the action buttons to the bottom of the screen if there is empty space
            Spacer(modifier = Modifier.fillMaxWidth().weight(1f))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onFinish) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onSaveClick) {
                    Text("Save")
                }
            }
        }
    }
    if (showProgressDialog) {
        ComposeDialog(
            onOk = null,
            onCancel = null,
            onDismiss = {},
            title = stringResource(R.string.copy_progress),
            content = {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator()
                }
            }
        )
    }
}

@Composable
fun FileItem(text: String, onDismiss: () -> Unit) {
    Surface(
        tonalElevation = 8.dp,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text, Modifier.padding(horizontal = 8.dp))
            IconButton(onDismiss, Modifier.size(24.dp)) {
                Icon(
                    Icons.Outlined.Close,
                    stringResource(R.string.delete_folder),
                    Modifier.size(16.dp)
                )
            }
        }
    }
}

@Preview(showSystemUi = true, showBackground = true, uiMode = ThemeControls.UI_MODE)
@Composable
fun ShareScreenPreview() {
    SyncthingandroidTheme(ThemeControls.useDarkMode, dynamicColor = ThemeControls.isMonetEnabled) {
        ShareScreen(
            files = mutableMapOf("~/test.txt".toUri() to "test.txt"),
            folders = listOf(Folder(label = "Folder 1"), Folder(label = "Folder 2"), Folder(label = "Folder 3")),
            selectedFolder = 0,
            subDirectory = "Sub folder",
            isMultipleFiles = false,
            showProgressDialog = false,
            copyResult = null,
            onFolderSelect = {},
            onFileRemove = {},
            onBrowseClick = {},
            onSaveClick = {},
            onFinish = {}
        )
    }
}

@Preview(showBackground = true, uiMode = ThemeControls.UI_MODE)
@Composable
fun FileItemPreview() {
    SyncthingandroidTheme(ThemeControls.useDarkMode, dynamicColor = ThemeControls.isMonetEnabled) {
        FileItem("this is a file bruh") {}
    }
}
