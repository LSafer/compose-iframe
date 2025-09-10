package net.lsafer.compose.iframe.internal

import dev.datlag.kcef.KCEFBrowser
import dev.datlag.kcef.KCEFClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import net.lsafer.compose.iframe.IframeIncomingEvent
import net.lsafer.compose.iframe.IframeOutgoingEvent
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.browser.CefMessageRouter
import org.cef.callback.CefQueryCallback
import org.cef.handler.CefMessageRouterHandlerAdapter
import kotlin.coroutines.resume

/* ============= ------------------ ============= */

// language=javascript
private const val INTEROP_INSTALL_SCRIPT = """
if (!window.kmpInteropEstablished) {
    window.parent.addEventListener("message", event => {
        const iframeEvent = { data: event.data, origin: event.origin }
        window.cefQuery({ request: JSON.stringify(iframeEvent) })
    })
    window.kmpInteropEstablished = true
}
"""

internal fun KCEFBrowser.executeInteropInstallScript() {
    evaluateJavaScript(INTEROP_INSTALL_SCRIPT) {}
}

/* ============= ------------------ ============= */

internal fun KCEFClient.installIncomingChannel(
    coroutineScope: CoroutineScope,
    channel: SendChannel<IframeIncomingEvent>,
) {
    val bridge = object : CefMessageRouterHandlerAdapter() {
        override fun onQuery(
            browser: CefBrowser?,
            frame: CefFrame?,
            queryId: Long,
            request: String?,
            persistent: Boolean,
            callback: CefQueryCallback?
        ): Boolean {
            if (request == null) return false

            val iframeEvent: IframeIncomingEvent =
                Json.decodeFromString(request)

            coroutineScope.launch(Dispatchers.IO) {
                channel.send(iframeEvent)
            }
            callback?.success("")
            return true
        }
    }

    val router = CefMessageRouter.create()
    router.addHandler(bridge, false)
    addMessageRouter(router)
}

/* ============= ------------------ ============= */

internal fun KCEFBrowser.installOutgoingChannel(
    coroutineScope: CoroutineScope,
    channel: ReceiveChannel<IframeOutgoingEvent>,
) {
    coroutineScope.launch {
        for (iframeEvent in channel) {
            val dataString = Json.encodeToString(iframeEvent.data)
            val targetOrigin = Json.encodeToString(iframeEvent.targetOrigin)

            // language=javascript
            val script = "window.postMessage($dataString, $targetOrigin)"

            suspendCancellableCoroutine { cont ->
                evaluateJavaScript(script) {
                    cont.resume(Unit)
                }
            }
        }
    }
}

/* ============= ------------------ ============= */
