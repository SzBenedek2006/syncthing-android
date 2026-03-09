package dev.benedek.syncthingandroid.ui.settings.categories

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.ui.reusable.preventClicksWhenExiting
import dev.benedek.syncthingandroid.ui.settings.SettingsViewModel
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