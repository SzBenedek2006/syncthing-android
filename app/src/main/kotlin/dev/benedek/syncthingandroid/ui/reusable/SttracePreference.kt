package dev.benedek.syncthingandroid.ui.reusable

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.preference.PreferenceManager
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.logic.util.SttraceUtil
import me.zhanghai.compose.preference.multiSelectListPreference
import androidx.compose.runtime.State

data class SttraceState(
    val facilities: List<String> = emptyList()
)

@Composable
fun rememberSttraceState(): State<SttraceState> {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    return produceState(initialValue = SttraceState()) {
        val facilities = SttraceUtil.getDebugFacilities(prefs)
        value = SttraceState(facilities)
    }
}

fun LazyListScope.sttracePreference(
    state: SttraceState,
    key: String
) {
    multiSelectListPreference(
        key = key,
        defaultValue = emptySet(),
        values = state.facilities,
        title = { Text(stringResource(R.string.sttrace_title)) },
        valueToText = { value -> AnnotatedString(value) }
    )
}