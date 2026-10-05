package dev.benedek.syncthingandroid.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.benedek.syncthingandroid.ui.reusable.AppScaffold
import dev.benedek.syncthingandroid.ui.theme.SyncthingandroidTheme
import dev.benedek.syncthingandroid.util.ThemeControls
import dev.benedek.syncthingandroid.R

/**
 * Share screen.
 * First draft was made by an LLM, based on activity_share.xml
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareScreen(
    // State
    fileNames: List<String>,
    folders: List<String>,
    selectedFolder: String,
    subDirectory: String,
    isMultipleFiles: Boolean,
    // Events
    onFileNameChange: (String) -> Unit,
    onFolderSelect: (String) -> Unit,
    onBrowseClick: () -> Unit,
    onCancelClick: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppScaffold(
        modifier = modifier.fillMaxSize(),
        topAppBarTitle = stringResource(R.string.share_activity_title)
    ) { paddingValues ->

        // Replaces ScrollView + LinearLayout + ConstraintLayout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // File Name Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isMultipleFiles) stringResource(R.string.files_list) else stringResource(R.string.file_name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column() {
                    fileNames.forEach { fileName ->
                        Text(fileName)
                    }
                }

            }

            // Folders Spinner Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Folder",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                var expanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedFolder,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        folders.forEach { folder ->
                            DropdownMenuItem(
                                text = { Text(folder) },
                                onClick = {
                                    onFolderSelect(folder)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Sub-directory Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Sub folder",
                    style = MaterialTheme.typography.bodyMedium,
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
                TextButton(onClick = onCancelClick) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onSaveClick) {
                    Text("Save")
                }
            }
        }
    }
}

@Preview(showSystemUi = true, showBackground = true, uiMode = ThemeControls.UI_MODE)
@Composable
fun ShareScreenPreview() {
    SyncthingandroidTheme(ThemeControls.useDarkMode, dynamicColor = ThemeControls.isMonetEnabled) {
        ShareScreen(
            fileNames = listOf("test.txt"),
            folders = listOf("Folder 1", "Folder 2", "Folder 3"),
            selectedFolder = "Folder 1",
            subDirectory = "Sub folder",
            isMultipleFiles = false,
            onFileNameChange = {},
            onFolderSelect = {},
            onBrowseClick = {},
            onCancelClick = {},
            onSaveClick = {}
        )
    }
}