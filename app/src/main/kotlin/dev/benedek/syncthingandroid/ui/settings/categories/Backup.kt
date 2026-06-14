package dev.benedek.syncthingandroid.ui.settings.categories

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.ui.reusable.SettingsAlertDialog
import dev.benedek.syncthingandroid.ui.reusable.preventClicksWhenExiting
import dev.benedek.syncthingandroid.ui.settings.SettingsViewModel
import me.zhanghai.compose.preference.preference

@Composable
fun Backup(contentPadding: PaddingValues, viewModel: SettingsViewModel) {
	val context = LocalContext.current
	val showImportDialog = remember { mutableStateOf(false) }
	val showExportDialog = remember { mutableStateOf(false) }

	LazyColumn(
		modifier = Modifier
			.fillMaxSize()
			.preventClicksWhenExiting(),
		contentPadding = contentPadding
	) {
		preference(
			key = "export_config",
			title = {
				Row {
					Text(stringResource(R.string.export_config))
					Badge(
						containerColor = MaterialTheme.colorScheme.primaryContainer,
						contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
						modifier = Modifier.padding(start = 4.dp)
					) {
						Text(stringResource(R.string.beta))
					}
				}
			},
			onClick = { showExportDialog.value = true },
			enabled = viewModel.isApiAvailable
		)
		preference(
			key = "import_config",
			title = {
				Row {
					Text(stringResource(R.string.import_config))
					Badge(
						containerColor = MaterialTheme.colorScheme.primaryContainer,
						contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
						modifier = Modifier.padding(start = 4.dp)
					) {
						Text(stringResource(R.string.beta))
					}
				}
			},
			onClick = { showImportDialog.value = true },
			enabled = viewModel.isApiAvailable
		)
	}

	SettingsAlertDialog(
		text = stringResource(R.string.dialog_confirm_import),
		confirmAction = { viewModel.importConfig(context) },
		showDialog = showImportDialog,
	)

	SettingsAlertDialog(
		text = stringResource(R.string.dialog_confirm_export),
		confirmAction = { viewModel.exportConfig(context) },
		showDialog = showExportDialog,
	)
}