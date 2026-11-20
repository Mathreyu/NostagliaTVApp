package com.example.newtv.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester

/** Requests focus (after the first layout pass) whenever [key] changes and [enabled] is true. */
@Composable
fun rememberInitialFocus(key: Any? = Unit, enabled: Boolean = true): FocusRequester {
    val requester = remember { FocusRequester() }
    LaunchedEffect(key, enabled) {
        if (enabled) {
            withFrameNanos { }
            requester.tryRequestFocus()
        }
    }
    return requester
}

/** requestFocus() throws when the target is not attached (yet); focus then falls back to traversal. */
fun FocusRequester.tryRequestFocus(): Boolean = try {
    requestFocus()
} catch (_: IllegalStateException) {
    false
} catch (_: IllegalArgumentException) {
    false
}
