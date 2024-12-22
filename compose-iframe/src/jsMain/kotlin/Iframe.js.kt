package net.lsafer.compose.iframe

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.w3c.dom.HTMLIFrameElement
import org.w3c.dom.MessageEvent
import org.w3c.dom.events.Event

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class IframeState(
    val iframe: HTMLIFrameElement,
    internal val _incoming: Channel<IframeIncomingEvent>,
    internal val _outgoing: Channel<IframeOutgoingEvent>,
) {
    actual val incoming: ReceiveChannel<IframeIncomingEvent> = _incoming
    actual val outgoing: SendChannel<IframeOutgoingEvent> = _outgoing
}

@Composable
actual fun rememberIframeState(url: String): IframeState {
    val iframe = remember(url) {
        document.createElement("iframe")
                as HTMLIFrameElement
    }.apply {
        src = url
    }
    val incoming = remember { Channel<IframeIncomingEvent>() }
    val outgoing = remember { Channel<IframeOutgoingEvent>() }

    return remember(
        incoming,
        outgoing,
    ) {
        IframeState(
            iframe = iframe,
            _incoming = incoming,
            _outgoing = outgoing,
        )
    }
}

@Composable
actual fun Iframe(state: IframeState, modifier: Modifier) {
    val scope = rememberCoroutineScope()

    // attach / detach iframe
    DisposableEffect(state.iframe) {
        document.body!!.appendChild(state.iframe)
        onDispose { document.body!!.removeChild(state.iframe) }
    }

    // inject interop script
    DisposableEffect(state.iframe) {
        val callback: (Event) -> Unit = { event ->
            event as MessageEvent
            val iframeEvent = IframeIncomingEvent(
                data = Json.parseToJsonElement(JSON.stringify(event.data)),
                origin = event.origin,
            )
            scope.launch { state._incoming.send(iframeEvent) }
        }
        window.addEventListener("message", callback)
        onDispose { window.removeEventListener("message", callback) }
    }

    // dispose outgoing messages
    LaunchedEffect(state.iframe) {
        launch {
            for (iframeEvent in state._outgoing) {
                val dataObject: dynamic = JSON.parse(Json.encodeToString(iframeEvent.data))
                val targetOrigin = iframeEvent.targetOrigin

                state.iframe.contentWindow!!.postMessage(dataObject, targetOrigin)
            }
        }
    }

    Box(modifier.onGloballyPositioned { coordinates ->
        val bounds = coordinates.boundsInWindow()
        val ratio = window.devicePixelRatio

        state.iframe.style.width = "${coordinates.size.width / ratio}px"
        state.iframe.style.height = "${coordinates.size.height / ratio}px"
        state.iframe.style.top = "${bounds.top / ratio}px"
        state.iframe.style.left = "${bounds.left / ratio}px"
        state.iframe.style.right = "${bounds.right / ratio}px"
        state.iframe.style.bottom = "${bounds.bottom / ratio}px"
        state.iframe.style.position = "absolute"
        state.iframe.style.background = "white"
        state.iframe.style.border = "none"
    })
}
