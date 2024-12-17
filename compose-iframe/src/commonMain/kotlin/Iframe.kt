package net.lsafer.compose.iframe

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.serialization.json.JsonElement

data class IframeIncomingEvent(
    val data: JsonElement,
    val origin: String,
)

data class IframeOutgoingEvent(
    val data: JsonElement,
    val targetOrigin: String = "*",
)

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class IframeState {
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

/**
 * Create an `iframe` state with its `src` being `url`.
 */
@Composable
expect fun rememberIframeState(url: String): IframeState

/**
 * Render an `iframe` that uses [state].
 *
 * > Note: The `iframe` will be on top of everything when rendered in the browser
 *           a fix **might** be implemented but this is not a priority to us.
 */
@Composable
expect fun Iframe(state: IframeState, modifier: Modifier = Modifier)
