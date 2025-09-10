package net.lsafer.compose.iframe.internal

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import net.lsafer.compose.iframe.IframeIncomingEvent
import net.lsafer.compose.iframe.IframeOutgoingEvent
import org.w3c.dom.HTMLIFrameElement
import org.w3c.dom.MessageEvent
import org.w3c.dom.events.Event
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.js

/* ============= ------------------ ============= */

@OptIn(ExperimentalWasmJsInterop::class)
internal fun HTMLIFrameElement.installElement(coroutineScope: CoroutineScope) {
    style.setProperty("display", "none")
    document.body!!.appendChild(this)
    coroutineScope.coroutineContext.job.invokeOnCompletion {
        document.body!!.removeChild(this)
    }
}

/* ============= ------------------ ============= */

@OptIn(ExperimentalWasmJsInterop::class)
internal fun HTMLIFrameElement.installIncomingChannel(
    coroutineScope: CoroutineScope,
    channel: SendChannel<IframeIncomingEvent>,
) {
    val callback: (Event) -> Unit = it@{ event ->
        event as MessageEvent
        if (event.source !== contentWindow) return@it
        val iframeEvent = IframeIncomingEvent(
            data = Json.parseToJsonElement(JSON_stringify(event.data)),
            origin = event.origin,
        )
        coroutineScope.launch { channel.send(iframeEvent) }
    }

    window.addEventListener("message", callback)

    coroutineScope.coroutineContext.job.invokeOnCompletion {
        window.removeEventListener("message", callback)
    }
}

/* ============= ------------------ ============= */

@OptIn(ExperimentalWasmJsInterop::class)
internal fun HTMLIFrameElement.installOutgoingChannel(
    coroutineScope: CoroutineScope,
    channel: ReceiveChannel<IframeOutgoingEvent>,
) {
    coroutineScope.launch {
        for (iframeEvent in channel) {
            val dataObject = JSON_parse(Json.encodeToString(iframeEvent.data))
            val targetOrigin = iframeEvent.targetOrigin

            contentWindow!!.postMessage(dataObject, targetOrigin)
        }
    }
}

/* ============= ------------------ ============= */

@OptIn(ExperimentalWasmJsInterop::class)
@Suppress("FunctionName")
private fun JSON_parse(value: String): JsAny = js("JSON.parse(value)")

@OptIn(ExperimentalWasmJsInterop::class)
@Suppress("FunctionName")
private fun JSON_stringify(value: JsAny?): String = js("JSON.stringify(value)")
