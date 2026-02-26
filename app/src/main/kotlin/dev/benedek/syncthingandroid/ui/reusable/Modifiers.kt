package dev.benedek.syncthingandroid.ui.reusable

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner

fun Modifier.preventClicksWhenExiting(): Modifier = composed {
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()

    this.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                // https://developer.android.com/guide/components/activities/activity-lifecycle#onresume
                if (!lifecycleState.isAtLeast(Lifecycle.State.RESUMED)) {
                    event.changes.forEach { it.consume() }
                }
            }
        }
    }
}