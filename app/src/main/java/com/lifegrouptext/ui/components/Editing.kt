package com.lifegrouptext.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged

/**
 * Tracks whether any text field in the app currently has focus.
 *
 * The obvious alternative, `WindowInsets.isImeVisible`, reports the keyboard as visible
 * even when it is not unless the window opts into full inset dispatch — which would
 * leave the bottom bar permanently stuck in its editing state. Focus is something this
 * app controls directly, and it is the condition we actually care about.
 */
@Stable
class EditingTracker {
    private val focusedFields = mutableStateMapOf<Any, Unit>()

    val isEditing: Boolean get() = focusedFields.isNotEmpty()

    fun setFocused(id: Any, focused: Boolean) {
        if (focused) focusedFields[id] = Unit else focusedFields.remove(id)
    }
}

val LocalEditingTracker = compositionLocalOf { EditingTracker() }

/**
 * Report this field's focus to the app-wide [EditingTracker]. A field that leaves the
 * composition while focused is forgotten, so the flag cannot stick on.
 */
fun Modifier.trackEditing(): Modifier = composed {
    val tracker = LocalEditingTracker.current
    val id = remember { Any() }
    DisposableEffect(id) {
        onDispose { tracker.setFocused(id, false) }
    }
    onFocusChanged { state -> tracker.setFocused(id, state.isFocused) }
}
