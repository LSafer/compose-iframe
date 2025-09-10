package net.lsafer.compose.iframe

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.view.View
import android.webkit.WebView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import net.lsafer.compose.iframe.internal.*

actual fun IframeState(coroutineScope: CoroutineScope): IframeState {
    return IframeState(
        onCreateWebView = { context -> WebView(context) },
        coroutineScope = coroutineScope,
    )
}

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class IframeState(
    private val onCreateWebView: (Context) -> WebView,
    private val coroutineScope: CoroutineScope,
) {
    private var _initialSrc: String = "about:blank"
    private var _src by mutableStateOf<String?>(null)
    private var _isLoading by mutableStateOf(true)
    private val _incoming = Channel<IframeIncomingEvent>()
    private val _outgoing = Channel<IframeOutgoingEvent>()

    internal var canGoBack by mutableStateOf(false)
        private set

    actual val isLoading get() = _isLoading

    actual var src: String
        get() = _src ?: _initialSrc
        @Synchronized set(value) {
            if (::webView.isInitialized) {
                webView.loadUrl(value)
            } else {
                _initialSrc = value
            }
        }

    actual val incoming: ReceiveChannel<IframeIncomingEvent> = _incoming
    actual val outgoing: SendChannel<IframeOutgoingEvent> = _outgoing

    lateinit var webView: WebView
        private set

    val isReady get() = ::webView.isInitialized

    @Synchronized
    internal fun getOrCreateWebView(context: Context): WebView {
        if (::webView.isInitialized) return webView
        val newWebView = onCreateWebView(context)
        webView = newWebView
        newWebView.installIncomingChannel(coroutineScope, _incoming)
        newWebView.installOutgoingChannel(coroutineScope, _outgoing)
        newWebView.webViewClient = WebViewClientImpl(
            onLoadStarted = {
                _isLoading = true
                canGoBack = newWebView.canGoBack()
            },
            onLoadEnd = {
                _isLoading = false
                newWebView.executeInteropInstallScript()
                canGoBack = newWebView.canGoBack()
            },
            onAddressChange = {
                _src = it
                canGoBack = newWebView.canGoBack()
            },
        )
        newWebView.webChromeClient = WebChromeClientImpl(
            context = context,
            coroutineScope = coroutineScope,
            onLoadStarted = { _isLoading = true },
            onLoadEnd = {
                _isLoading = false
                newWebView.executeInteropInstallScript()
                canGoBack = newWebView.canGoBack()
            },
            onAddressChange = {
                _src = it
                canGoBack = newWebView.canGoBack()
            }
        )

        newWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        newWebView.settings.javaScriptEnabled = true
        newWebView.settings.userAgentString = null
        newWebView.settings.allowFileAccessFromFileURLs = false
        newWebView.settings.allowUniversalAccessFromFileURLs = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            newWebView.settings.safeBrowsingEnabled = true
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            newWebView.settings.isAlgorithmicDarkeningAllowed = false
        }
        newWebView.setBackgroundColor(Color.TRANSPARENT)
        newWebView.settings.allowFileAccess = false
        newWebView.settings.textZoom = 100
        newWebView.settings.useWideViewPort = false
        newWebView.settings.standardFontFamily = "sans-serif"
        newWebView.settings.defaultFontSize = 16
        newWebView.settings.loadsImagesAutomatically = true
        newWebView.settings.domStorageEnabled = true // changed from accompanist default
        newWebView.settings.mediaPlaybackRequiresUserGesture = true

        @Suppress("Deprecation")
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            val nightModeFlags = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
                WebSettingsCompat.setForceDark(newWebView.settings, WebSettingsCompat.FORCE_DARK_ON)
            } else {
                WebSettingsCompat.setForceDark(newWebView.settings, WebSettingsCompat.FORCE_DARK_OFF)
            }

            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
                WebSettingsCompat.setForceDarkStrategy(
                    newWebView.settings,
                    WebSettingsCompat.DARK_STRATEGY_WEB_THEME_DARKENING_ONLY,
                )
            }
        }

        webView.loadUrl(_initialSrc)
        return newWebView
    }
}
