package net.lsafer.compose.iframe

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.datlag.kcef.KCEF
import dev.datlag.kcef.KCEFBrowser
import dev.datlag.kcef.KCEFClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.job
import net.lsafer.compose.iframe.internal.*
import org.cef.browser.CefRendering

actual fun IframeState(coroutineScope: CoroutineScope): IframeState {
    return IframeState(
        onCreateClient = { KCEF.newClientBlocking() },
        coroutineScope = coroutineScope,
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class IframeState(
    private val onCreateClient: () -> KCEFClient,
    private val coroutineScope: CoroutineScope,
) {
    private var _initialSrc: String = KCEFBrowser.BLANK_URI
    private var _src: String? by mutableStateOf(null)
    private var _isLoading: Boolean by mutableStateOf(true)
    private val _incoming = Channel<IframeIncomingEvent>()
    private val _outgoing = Channel<IframeOutgoingEvent>()

    actual val isLoading get() = _isLoading

    actual var src: String
        get() = _src ?: _initialSrc
        @Synchronized set(value) {
            if (::browser.isInitialized) {
                browser.loadURL(value)
            } else {
                _initialSrc = value
            }
        }

    actual val incoming: ReceiveChannel<IframeIncomingEvent> = _incoming
    actual val outgoing: SendChannel<IframeOutgoingEvent> = _outgoing

    lateinit var client: KCEFClient
        private set
    lateinit var browser: KCEFBrowser
        private set

    val isReady get() = ::client.isInitialized && ::browser.isInitialized

    @Synchronized
    fun getOrCreateClient(): KCEFClient {
        if (::client.isInitialized) return client
        val newClient = onCreateClient()
        client = newClient
        newClient.installIncomingChannel(coroutineScope, _incoming)
        newClient.addDisplayHandler(DisplayHandlerImpl(
            onAddressChange = {
                _src = it
                if (::browser.isInitialized)
                    browser.executeInteropInstallScript()
            },
        ))
        newClient.addLoadHandler(LoadHandlerImpl(
            onLoadingStart = { _isLoading = true },
            onLoadingEnd = {
                _isLoading = false
                if (::browser.isInitialized)
                    browser.executeInteropInstallScript()
            },
        ))
        newClient.addDialogHandler(DialogHandlerImpl(coroutineScope))
        coroutineScope.coroutineContext.job.invokeOnCompletion {
            newClient.dispose()
        }
        return newClient
    }

    @Synchronized
    fun getOrCreateBrowser(): KCEFBrowser {
        if (::browser.isInitialized) return browser
        val client = getOrCreateClient()
        val newBrowser = client.createBrowser(
            url = _initialSrc,
            rendering = CefRendering.DEFAULT,
            isTransparent = true,
        )
        browser = newBrowser
        newBrowser.installOutgoingChannel(coroutineScope, _outgoing)
        coroutineScope.coroutineContext.job.invokeOnCompletion {
            newBrowser.dispose()
        }
        return newBrowser
    }
}
