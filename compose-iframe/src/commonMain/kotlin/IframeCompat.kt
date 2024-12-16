package net.lsafer.compose.iframe

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.multiplatform.webview.jsbridge.IJsMessageHandler
import com.multiplatform.webview.jsbridge.JsMessage
import com.multiplatform.webview.jsbridge.WebViewJsBridge
import com.multiplatform.webview.web.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlin.coroutines.resume

data class IframeEvent(val data: JsonElement)

class IframeState(
    val webview: WebViewState,
    val navigator: WebViewNavigator,
    val jsBridge: WebViewJsBridge,
    internal val incomingCh: Channel<IframeEvent>,
    internal val outgoingCh: Channel<IframeEvent>,
) {
    val incoming: ReceiveChannel<IframeEvent> = incomingCh
    val outgoing: SendChannel<IframeEvent> = outgoingCh
}

@Composable
fun rememberIframeState(
    url: String,
    additionalHttpHeaders: Map<String, String> = emptyMap(),
): IframeState {
    val webview = rememberWebViewState(url, additionalHttpHeaders)
    val navigator = rememberWebViewNavigator()
    val jsBridge = remember { WebViewJsBridge() }
    val incoming = remember { Channel<IframeEvent>() }
    val outgoing = remember { Channel<IframeEvent>() }

    return remember(
        webview,
        navigator,
        jsBridge,
        incoming,
        outgoing,
    ) {
        IframeState(
            webview = webview,
            navigator = navigator,
            jsBridge = jsBridge,
            incomingCh = incoming,
            outgoingCh = outgoing,
        )
    }
}

@Composable
fun IframeCompat(
    state: IframeState,
    modifier: Modifier = Modifier,
    captureBackPresses: Boolean = true,
    onCreated: () -> Unit = {},
    onDispose: () -> Unit = {},
    platformWebViewParams: PlatformWebViewParams? = null,
) {
    val scope = rememberCoroutineScope()

    // establish jsBridge
    DisposableEffect(
        state.jsBridge,
        state.webview.loadingState,
    ) {
        if (state.webview.loadingState != LoadingState.Finished)
            return@DisposableEffect onDispose {}

        val jsBridgeHandler = object : IJsMessageHandler {
            override fun methodName() = "onMessageReceived"

            override fun handle(
                message: JsMessage,
                navigator: WebViewNavigator?,
                callback: (String) -> Unit
            ) {
                val data = Json.parseToJsonElement(message.params)
                scope.launch(Dispatchers.IO) {
                    state.incomingCh.send(IframeEvent(data))
                }
                callback("")
            }
        }

        state.jsBridge.register(jsBridgeHandler)
        onDispose { state.jsBridge.unregister(jsBridgeHandler) }
    }

    // inject interop script
    LaunchedEffect(
        state.navigator,
        state.webview.content,
        state.webview.loadingState,
    ) {
        if (state.webview.loadingState != LoadingState.Finished)
            return@LaunchedEffect

        // language=javascript
        val script = """
            if (!window.kmpInteropEstablished) {
                window.parent.addEventListener("message", event => {
                    window.kmpJsBridge.callNative("onMessageReceived", JSON.stringify(event.data))
                })
                window.kmpInteropEstablished = true
            }
        """.trimIndent()

        state.navigator.evaluateJavaScript(script)
    }

    // dispose outgoing messages
    LaunchedEffect(state.webview.loadingState) {
        if (state.webview.loadingState != LoadingState.Finished)
            return@LaunchedEffect

        launch {
            for (message in state.outgoingCh) {
                // language=javascript
                val data = Json.encodeToString(message.data)
                val script = "window.postMessage($data, '*')"

                suspendCancellableCoroutine { cont ->
                    state.navigator.evaluateJavaScript(script) {
                        cont.resume(Unit)
                    }
                }
            }
        }
    }

    WebViewCompat(
        state = state.webview,
        modifier = modifier,
        captureBackPresses = captureBackPresses,
        navigator = state.navigator,
        webViewJsBridge = state.jsBridge,
        onCreated = onCreated,
        onDispose = onDispose,
        platformWebViewParams = platformWebViewParams,
    )
}
