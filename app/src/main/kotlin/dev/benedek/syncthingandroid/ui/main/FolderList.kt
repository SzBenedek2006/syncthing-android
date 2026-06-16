package dev.benedek.syncthingandroid.ui.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.activities.FolderActivity
import dev.benedek.syncthingandroid.model.Folder
import dev.benedek.syncthingandroid.model.FolderStatus
import dev.benedek.syncthingandroid.service.Constants
import dev.benedek.syncthingandroid.service.SyncthingService
import dev.benedek.syncthingandroid.viewmodel.FolderViewModel
import dev.benedek.syncthingandroid.ui.theme.SyncthingandroidTheme
import dev.benedek.syncthingandroid.ui.theme.extendedColorScheme
import dev.benedek.syncthingandroid.util.ThemeControls
import dev.benedek.syncthingandroid.util.Util.readableFileSize
import java.io.File
import kotlin.math.roundToInt


@Composable
fun FolderList(
	folders: List<Folder> = emptyList(),
	folderStatuses: Map<String, FolderStatus> = emptyMap(),
	isLoaded: Boolean
) {
	if (!isLoaded) {
		Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
	} else if (folders.isEmpty()) {
		Box(Modifier.fillMaxSize(), Alignment.Center) {
			Text(text = stringResource(R.string.folder_list_empty))
		}
	} else {
		LazyColumn(Modifier.fillMaxSize()) {
			items(
				folders,
				key = { it.id ?: it.hashCode() } // TODO: Test perf.
			) { folder ->
				folderStatuses[folder.id]?.let { FolderListItem(folder, it) }
			}
		}
	}
}


@Composable
fun FolderListItem(
	folder: Folder,
	folderStatus: FolderStatus
) {
	val context = LocalContext.current
	Row(
		Modifier
			.fillMaxWidth()
			.clickable(
				onClickLabel = folder.label,
				onClick = {
					val intent = Intent(context, FolderActivity::class.java)
						.putExtra(FolderViewModel.EXTRA_IS_CREATE, false)
						.putExtra(FolderViewModel.EXTRA_FOLDER_ID, folder.id)
					context.startActivity(intent)
				}
			)
			.padding(16.dp, 8.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		Column(Modifier.weight(1f)) {
			Row(
				Modifier.fillMaxWidth(),
				Arrangement.SpaceBetween,
				Alignment.CenterVertically
			) {
				Text(
					text = folder.label?.ifEmpty { folder.id } ?: "",
					modifier = Modifier.weight(1f),
					style = MaterialTheme.typography.titleMedium,
					maxLines = 1
				)
			}

			Text(
				text = folder.path.orEmpty(),
				modifier = Modifier.padding(top = 4.dp),
				style = MaterialTheme.typography.bodySmall
			)

			val neededItems = folderStatus.needFiles + folderStatus.needDirectories +
					folderStatus.needSymlinks + folderStatus.needDeletes
			val outOfSync = folderStatus.state == "idle" && neededItems > 0
			val showOverride = folder.type == Constants.FOLDER_TYPE_SEND_ONLY && outOfSync
			val context = LocalContext.current
			if (showOverride) {
				Button(
					onClick = {
						val intent = Intent(context, SyncthingService::class.java)
							.putExtra(SyncthingService.EXTRA_FOLDER_ID, folder.id)
						intent.setAction(SyncthingService.ACTION_OVERRIDE_CHANGES)
						context.startService(intent)
					},
					modifier = Modifier.padding(top = 8.dp)
				) {
					Text(stringResource(R.string.override_changes))
				}
			}

			Text(
				text = LocalResources.current.getQuantityString(
					R.plurals.files,
					folderStatus.inSyncFiles.toInt(),
					folderStatus.inSyncFiles,
					folderStatus.globalFiles
				),
				modifier = Modifier.padding(top = 4.dp),
				style = MaterialTheme.typography.bodySmall
			)
			Text(
				text = LocalResources.current.getString(
					R.string.folder_size_format,
					readableFileSize(LocalContext.current, folderStatus.inSyncBytes),
					readableFileSize(LocalContext.current, folderStatus.globalBytes)
				),
				style = MaterialTheme.typography.bodySmall
			)

			Column {
				val state = getLocalizedState(context, folderStatus)
				val color = getStatusColor(folderStatus)
				Text(
					text = state,
					color = color,
					style = MaterialTheme.typography.labelMedium
				)
				// Invalid state
				val invalidMsg = folderStatus.invalid ?: folder.invalid
				if (!invalidMsg.isNullOrEmpty()) {
					Text(
						text = invalidMsg,
						color = Color.Red,
						style = MaterialTheme.typography.bodySmall
					)
				}
			}

		}
		val openFileManager = stringResource(R.string.open_file_manager)

		IconButton(
			onClick = {
				val intent = Intent(Intent.ACTION_VIEW)
				intent.setDataAndType(Uri.fromFile(File(folder.path!!)), "resource/folder")
				intent.putExtra("org.openintents.extra.ABSOLUTE_PATH", folder.path)
				intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
				if (intent.resolveActivity(context.packageManager) != null) {
					context.startActivity(intent)
				} else {
					Log.v(
						"FolderListItem",
						"openFolder: Fallback to application chooser to open folder."
					)
					intent.setDataAndType(folder.path!!.toUri(), "application/*")
					val chooserIntent = Intent.createChooser(intent, openFileManager)
					if (chooserIntent != null) {
						context.startActivity(chooserIntent)
					} else {
						Toast.makeText(context, R.string.toast_no_file_manager, Toast.LENGTH_SHORT).show()
					}
				}
			}
		) {
			Icon(
				painterResource(R.drawable.ic_folder_24dp),
				stringResource(R.string.open_file_manager)
			)
		}
	}
}

@Composable
fun getStatusColor(folderStatus: FolderStatus): Color {
	val blue = MaterialTheme.extendedColorScheme.blue.color
	val green = MaterialTheme.extendedColorScheme.green.color
	val red = MaterialTheme.extendedColorScheme.red.color
	val yellow = MaterialTheme.extendedColorScheme.yellow.color



	return remember(folderStatus) {
		val neededItems =
			folderStatus.needFiles + folderStatus.needDirectories + folderStatus.needSymlinks + folderStatus.needDeletes
		val outOfSync = folderStatus.state == "idle" && neededItems > 0

		if (outOfSync) Color.Red else
			when (folderStatus.state) {
				"idle" -> green
				"scanning", "syncing" -> blue
				"error" -> red
				else -> yellow
			}
	}
}

@Composable
fun getLocalizedState(context: Context, folderStatus: FolderStatus): String {
	return remember(folderStatus, context) {
		val neededItems =
			folderStatus.needFiles + folderStatus.needDirectories + folderStatus.needSymlinks + folderStatus.needDeletes
		val outOfSync = folderStatus.state == "idle" && neededItems > 0

		when (folderStatus.state) {
			"idle" -> {
				if (outOfSync) context.getString(R.string.status_outofsync)
				else context.getString(R.string.state_idle)
			}

			"scanning" -> context.getString(R.string.state_scanning)
			"syncing" -> {
				val percentage = if (folderStatus.globalBytes != 0L)
					(100f * folderStatus.inSyncBytes / folderStatus.globalBytes).roundToInt()
				else
					100
				context.getString(R.string.state_syncing, percentage)
			}

			"error" -> {
				if (TextUtils.isEmpty(folderStatus.error)) {
					context.getString(R.string.state_error)
				}
				context.getString(R.string.state_error) + " (" + folderStatus.error + ")"
			}

			"unknown" -> context.getString(R.string.state_unknown)
			else -> folderStatus.state.toString()
		}
	}
}


@Preview(showBackground = true, uiMode = ThemeControls.UI_MODE)
@Composable
fun FolderListPreview() {
	SyncthingandroidTheme(ThemeControls.PREVIEW_DARK_THEME, ThemeControls.isMonetEnabled) {
		Surface { FolderList(emptyList(), emptyMap(), true) }
	}
}

@Preview(showBackground = true, uiMode = ThemeControls.UI_MODE)
@Composable
fun FolderListItemPreview() {
	SyncthingandroidTheme(ThemeControls.PREVIEW_DARK_THEME, ThemeControls.isMonetEnabled) {
		Surface(contentColor = MaterialTheme.colorScheme.onSurface) {
			FolderListItem(
				Folder(
					id = "fjdlaf-jfdlaf",
					label = "Mao",
					path = "storage/emulated/0"
				),
				folderStatus = FolderStatus(
					state = "scanning",
					invalid = "Message here"
				)
			)
		}
	}
}

