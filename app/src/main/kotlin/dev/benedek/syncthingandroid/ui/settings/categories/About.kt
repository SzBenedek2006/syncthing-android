package dev.benedek.syncthingandroid.ui.settings.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.ui.icons.SyncthingForAndroidNew
import dev.benedek.syncthingandroid.ui.reusable.OptionTile
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

	val windowInfo = LocalWindowInfo.current
	val isLandscape = windowInfo.containerSize.width > windowInfo.containerSize.height

	Column(
		modifier = Modifier
			.fillMaxSize()
			.preventClicksWhenExiting()
			.verticalScroll(rememberScrollState())
			.padding(contentPadding)
	) {
		if (!isLandscape) {
			Column(
				Modifier
					.fillMaxWidth()
					.padding(24.dp),
				Arrangement.Center,
				Alignment.CenterHorizontally
			) {
				Icon(
					rememberVectorPainter(SyncthingForAndroidNew),
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
		} else {
			Row(
				Modifier
					.fillMaxWidth()
					.padding(24.dp),
				Arrangement.Center,
				Alignment.CenterVertically
			) {
				Icon(
					rememberVectorPainter(SyncthingForAndroidNew),
					null,
					Modifier.fillMaxWidth(0.25f).aspectRatio(1f).padding(16.dp),
					MaterialTheme.colorScheme.primary
				)
				Text(
					"Syncthing for Android",
					textAlign = TextAlign.Center,
					style = MaterialTheme.typography.displaySmall
				)
			}
		}
		OptionTile(
			title = stringResource(R.string.syncthing_forum_title),
			description = stringResource(R.string.syncthing_forum_summary),
			titleStyle = MaterialTheme.typography.bodyLarge,
			descriptionStyle = MaterialTheme.typography.bodyMedium,
			onClick = { uriHandler.openUri(forumUrl) },
			noIconPadding = true
		)
		OptionTile(
			title = stringResource(R.string.report_issue_title),
			description = stringResource(R.string.report_issue_summary),
			titleStyle = MaterialTheme.typography.bodyLarge,
			descriptionStyle = MaterialTheme.typography.bodyMedium,
			onClick = { uriHandler.openUri(issuesUrl) },
			noIconPadding = true
		)
		OptionTile(
			title = stringResource(R.string.donate_title),
			description = stringResource(R.string.donate_summary),
			titleStyle = MaterialTheme.typography.bodyLarge,
			descriptionStyle = MaterialTheme.typography.bodyMedium,
			onClick = { uriHandler.openUri(donateUrl) },
			noIconPadding = true
		)
		OptionTile(
			title = stringResource(R.string.privacy_title),
			description = stringResource(R.string.privacy_summary),
			titleStyle = MaterialTheme.typography.bodyLarge,
			descriptionStyle = MaterialTheme.typography.bodyMedium,
			onClick = { uriHandler.openUri(privacyUrl) },
			noIconPadding = true
		)
		OptionTile(
			title = stringResource(R.string.syncthing_version_title),
			description = viewModel.syncthingVersion.value,
			titleStyle = MaterialTheme.typography.bodyLarge,
			descriptionStyle = MaterialTheme.typography.bodyMedium,
			noIconPadding = true
		)
		OptionTile(
			title = stringResource(R.string.app_version_title),
			description = viewModel.syncthingAppVersion.value,
			titleStyle = MaterialTheme.typography.bodyLarge,
			descriptionStyle = MaterialTheme.typography.bodyMedium,
			noIconPadding = true
		)
	}
}

@Preview(showBackground = true, uiMode = ThemeControls.UI_MODE)
@Composable
fun AboutVerticalPreview() {
	SyncthingandroidTheme(ThemeControls.PREVIEW_DARK_THEME) {
		ProvidePreferenceLocals {
			Scaffold { paddingValues ->
				About(paddingValues, viewModel())
			}
		}
	}
}

@Preview(showBackground = true, uiMode = ThemeControls.UI_MODE, widthDp = 800, heightDp = 400)
@Composable
fun AboutHorizontalPreview() {
	SyncthingandroidTheme(ThemeControls.PREVIEW_DARK_THEME) {
		ProvidePreferenceLocals {
			Scaffold { paddingValues ->
				About(paddingValues, viewModel())
			}
		}
	}
}
