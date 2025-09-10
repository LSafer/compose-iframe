package net.lsafer.compose.iframe

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel

expect fun IframeState(coroutineScope: CoroutineScope): IframeState

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
