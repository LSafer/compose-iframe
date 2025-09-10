package net.lsafer.compose.iframe.internal

import android.webkit.JavascriptInterface
import android.webkit.WebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import net.lsafer.compose.iframe.IframeIncomingEvent
import net.lsafer.compose.iframe.IframeOutgoingEvent
import kotlin.coroutines.resume

/* ============= ------------------ ============= */

// language=javascript
private const val INTEROP_INSTALL_SCRIPT = """
if (!window.kmpInteropEstablished) {
    window.parent.addEventListener("message", event => {
        const iframeEvent = { data: event.data, origin: event.origin }
        window.androidJsBridge.post(JSON.stringify(iframeEvent))
    })
    window.kmpInteropEstablished = true
}
"""

internal fun WebView.executeInteropInstallScript() {
    evaluateJavascript(INTEROP_INSTALL_SCRIPT) {}
}

/* ============= ------------------ ============= */

fun WebView.installIncomingChannel(
    coroutineScope: CoroutineScope,
    channel: SendChannel<IframeIncomingEvent>,
) {
    val bridge = object {
        @JavascriptInterface
        fun post(message: String) {
            val iframeEvent: IframeIncomingEvent =
                Json.decodeFromString(message)

            coroutineScope.launch(Dispatchers.IO) {
                channel.send(iframeEvent)
            }
        }
    }

    addJavascriptInterface(bridge, "androidJsBridge")
}

/* ============= ------------------ ============= */

fun WebView.installOutgoingChannel(
    coroutineScope: CoroutineScope,
    channel: ReceiveChannel<IframeOutgoingEvent>,
) {
    coroutineScope.launch {
        for (iframeEvent in channel) {
            val dataString = Json.encodeToString(iframeEvent.data)
            val targetOrigin = iframeEvent.targetOrigin

            // language=javascript
            val script = "window.postMessage($dataString, $targetOrigin)"

            suspendCancellableCoroutine { cont ->
                evaluateJavascript(script) {
                    cont.resume(Unit)
                }
            }
        }
    }
}

/* ============= ------------------ ============= */
