package net.lsafer.compose.iframe

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import kotlinx.browser.document
import kotlinx.browser.window
import kotlin.js.ExperimentalWasmJsInterop

@OptIn(ExperimentalWasmJsInterop::class)
@Composable
actual fun Iframe(state: IframeState, modifier: Modifier) {
    // attach / detach iframe
    DisposableEffect(state.iframe) {
        document.body!!.appendChild(state.iframe)
        onDispose { document.body!!.removeChild(state.iframe) }
    }

    Box(modifier.onGloballyPositioned { coordinates ->
        val bounds = coordinates.boundsInWindow()
        val ratio = window.devicePixelRatio

        state.iframe.style.width = "${coordinates.size.width / ratio}px"
        state.iframe.style.height = "${coordinates.size.height / ratio}px"
        state.iframe.style.top = "${bounds.top / ratio}px"
        state.iframe.style.left = "${bounds.left / ratio}px"
//        state.iframe.style.right = "${bounds.right / ratio}px"
//        state.iframe.style.bottom = "${bounds.bottom / ratio}px"
        state.iframe.style.position = "absolute"
        state.iframe.style.background = "white"
        state.iframe.style.border = "none"
    })
}
