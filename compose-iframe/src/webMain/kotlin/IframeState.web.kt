package net.lsafer.compose.iframe

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.browser.document
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import net.lsafer.compose.iframe.internal.installIncomingChannel
import net.lsafer.compose.iframe.internal.installOutgoingChannel
import org.w3c.dom.HTMLIFrameElement
import kotlin.js.ExperimentalWasmJsInterop

actual fun IframeState(coroutineScope: CoroutineScope): IframeState {
    return IframeState(
        iframe = document.createElement("iframe") as HTMLIFrameElement,
        coroutineScope = coroutineScope,
    )
}

@OptIn(ExperimentalWasmJsInterop::class)
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class IframeState(
    val iframe: HTMLIFrameElement,
    private val coroutineScope: CoroutineScope,
) {
    private var _isLoading by mutableStateOf(false)
    private val _incoming = Channel<IframeIncomingEvent>()
    private val _outgoing = Channel<IframeOutgoingEvent>()

    actual val isLoading get() = _isLoading

    actual var src: String
        get() = iframe.src
        set(value) {
            _isLoading = true
            iframe.src = value
        }

    actual val incoming: ReceiveChannel<IframeIncomingEvent> = _incoming
    actual val outgoing: SendChannel<IframeOutgoingEvent> = _outgoing

    init {
        iframe.addEventListener("load") {
            _isLoading = false
        }
        iframe.installOutgoingChannel(coroutineScope, _outgoing)
        iframe.installIncomingChannel(coroutineScope, _incoming)
    }
}
