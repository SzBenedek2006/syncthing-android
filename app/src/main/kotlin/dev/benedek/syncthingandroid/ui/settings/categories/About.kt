package dev.benedek.syncthingandroid.ui.settings.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.ui.reusable.preventClicksWhenExiting
import dev.benedek.syncthingandroid.ui.theme.SyncthingandroidTheme
import dev.benedek.syncthingandroid.util.ThemeControls
import dev.benedek.syncthingandroid.viewmodel.SettingsViewModel
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.preference

@Composable
fun About(contentPadding: PaddingValues, viewModel: SettingsViewModel) {
	val uriHandler = LocalUriHandler.current
	val forumUrl = stringResource(R.string.syncthing_forum_url)
	val issuesUrl = stringResource(R.string.issue_tracker_url)
	val donateUrl = stringResource(R.string.donate_url)
	val privacyUrl = stringResource(R.string.privacy_policy_url)

	LazyColumn(
		modifier = Modifier
			.fillMaxSize()
			.preventClicksWhenExiting(),
		contentPadding = contentPadding
	) {
		item {
			Column(
				Modifier
					.fillMaxWidth()
					.padding(24.dp),
				Arrangement.Center,
				Alignment.CenterHorizontally
			) {
				Icon(
					painterResource(R.drawable.ic_syncthing_monochrome),
					null,
					Modifier.fillMaxWidth(0.5f).aspectRatio(1f).padding(16.dp),
					MaterialTheme.colorScheme.primary
				)
				Text(
					"Syncthing for Android",
					textAlign = TextAlign.Center,
					style = MaterialTheme.typography.displaySmall
				)
			}
		}
		preference(
			key = "syncthing_forum",  // #noKey
			title = { Text(stringResource(R.string.syncthing_forum_title)) },
			summary = { Text(stringResource(R.string.syncthing_forum_summary)) },
			onClick = { uriHandler.openUri(forumUrl) }
		)
		preference(
			key = "report_issue",  // #noKey
			title = { Text(stringResource(R.string.report_issue_title)) },
			summary = { Text(stringResource(R.string.report_issue_summary)) },
			onClick = { uriHandler.openUri(issuesUrl) }
		)
		preference(
			key = "donate",  // #noKey
			title = { Text(stringResource(R.string.donate_title)) },
			summary = { Text(stringResource(R.string.donate_summary)) },
			onClick = { uriHandler.openUri(donateUrl) }
		)
		preference(
			key = "privacy",  // #noKey
			title = { Text(stringResource(R.string.privacy_title)) },
			summary = { Text(stringResource(R.string.privacy_summary)) },
			onClick = { uriHandler.openUri(privacyUrl) }
		)
		preference(
			key = "syncthing_version",
			title = { Text(stringResource(R.string.syncthing_version_title)) },
			summary = { Text(viewModel.syncthingVersion.value) }
		)
		preference(
			key = "app_version",
			title = { Text(stringResource(R.string.app_version_title)) },
			summary = { Text(viewModel.syncthingAppVersion.value) }
		)
	}
}

@Preview(showBackground = true, uiMode = ThemeControls.UI_MODE)
@Composable
fun AboutPreview() {
	SyncthingandroidTheme(ThemeControls.PREVIEW_DARK_THEME) {
		ProvidePreferenceLocals {
			Scaffold { paddingValues ->
				About(paddingValues, viewModel())
			}
		}
	}
}