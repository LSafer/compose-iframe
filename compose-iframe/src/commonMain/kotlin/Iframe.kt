package net.lsafer.compose.iframe

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class IframeIncomingEvent(
    val data: JsonElement,
    val origin: String,
)

@Serializable
data class IframeOutgoingEvent(
    val data: JsonElement,
    val targetOrigin: String = "*",
)

/**
 * Create an `iframe` state with its `src` being `url`.
 */
@Composable
fun rememberIframeState(url: String): IframeState {
    val coroutineScope = rememberCoroutineScope()
    val iframe = remember(coroutineScope) { IframeState(coroutineScope) }

    LaunchedEffect(iframe, url) {
        iframe.src = url
    }

    return iframe
}

/**
 * Render an `iframe` that uses [state].
 *
 * > Note: The `iframe` will be on top of everything when rendered in the browser
 *           a fix **might** be implemented but this is not a priority to us.
 */
@Composable
expect fun Iframe(state: IframeState, modifier: Modifier = Modifier)
