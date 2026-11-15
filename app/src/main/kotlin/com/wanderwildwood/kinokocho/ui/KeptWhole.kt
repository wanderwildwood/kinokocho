package com.wanderwildwood.kinokocho.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onSizeChanged

/**
 * The text fields of a paged form, kept on the page while they are typed in.
 *
 * The window is resized for the keyboard (adjustResize in the manifest), so the bar stays at the
 * top and the list gives up the room. Each time the room changes, or the cursor goes into another
 * field, the field being typed in is brought whole onto the page; when the room comes back the
 * page starts on a whole row again first.
 */
internal class TypingIn {
    internal val field = mutableStateOf<BringIntoViewRequester?>(null)
    internal var listHeight by mutableIntStateOf(0)

    /** On the list: its height is how the keyboard's coming and going is seen. */
    fun listModifier(): Modifier = Modifier.onSizeChanged { listHeight = it.height }
}

@Composable
internal fun rememberTypingIn(list: LazyListState): TypingIn {
    val typing = remember { TypingIn() }
    LaunchedEffect(list) {
        var last = 0
        snapshotFlow { typing.listHeight to typing.field.value }.collect { (height, field) ->
            if (last in 1 until height) list.scrollToItem(list.firstVisibleItemIndex)
            last = height
            withFrameNanos { }
            field?.bringIntoView()
        }
    }
    return typing
}

/** Marks a text field, border and all, as the one to keep on the page while the cursor is in it. */
@Composable
internal fun Modifier.keptWhole(typing: TypingIn): Modifier {
    val typingIn: MutableState<BringIntoViewRequester?> = typing.field
    val whole = remember { BringIntoViewRequester() }
    DisposableEffect(whole) { onDispose { if (typingIn.value === whole) typingIn.value = null } }
    return bringIntoViewRequester(whole).onFocusChanged {
        if (it.isFocused) typingIn.value = whole else if (typingIn.value === whole) typingIn.value = null
    }
}
