package net.lsafer.compose.iframe

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
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

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class IframeState {
    /**
     * The current url loaded on the iframe.
     * Changing this variable will cause the
     * iframe to change its content.
     *
     * NOTE: this variable is intentionally unstable to mimic
     *     the behaviour of iframe. It may not be updated
     *     immediately and changes to it whether internally or by
     *     its setter may not cause recomposition.
     */
    var src: String

    /**
     * If the iframe is loading or not. (causes recomposition on change)
     */
    val isLoading: Boolean

    /**
     * Incoming messages to the parent window of the `iframe`.
     *
     * > Note: this is a channel of ALL the messages guaranteeing only
     *          one thing, it includes all messages from the `iframe`.
     */
    val incoming: ReceiveChannel<IframeIncomingEvent>

    /**
     * Outgoing messages to the window of the `iframe`.
     */
    val outgoing: SendChannel<IframeOutgoingEvent>
}

expect fun IframeState(coroutineScope: CoroutineScope): IframeState

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
