package net.lsafer.compose.iframe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.multiplatform.webview.jsbridge.WebViewJsBridge
import com.multiplatform.webview.web.*
import dev.datlag.kcef.KCEFBrowser

@Composable
actual fun WebViewCompat(
    state: WebViewState,
    modifier: Modifier,
    captureBackPresses: Boolean,
    navigator: WebViewNavigator,
    webViewJsBridge: WebViewJsBridge?,
    onCreated: () -> Unit,
    onDispose: () -> Unit,
    platformWebViewParams: PlatformWebViewParams?,
) {
    val startup = LocalJcefStartupState.current

    if (!startup.isInitialized) {
        Column(
            modifier.padding(8.dp),
            Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            Alignment.CenterHorizontally,
        ) {
            LinearProgressIndicator(
                progress = { startup.progress / 100 },
                modifier = Modifier.width(450.dp),
            )

            @Suppress("DefaultLocale")
            Text(String.format("%.2f%%", startup.progress))
        }
        return
    }

    CompositionLocalProvider(
        LocalDensity provides Density(1f, 1f)
    ) {
        WebView(
            state = state,
            modifier = modifier,
            captureBackPresses = captureBackPresses,
            navigator = navigator,
            webViewJsBridge = webViewJsBridge,
            onCreated = { onCreated() },
            onDispose = { onDispose() },
            platformWebViewParams = platformWebViewParams,
            factory = defaultFactory,
        )
    }
}

private val defaultFactory = { param: WebViewFactoryParam ->
    when (val content = param.state.content) {
        is WebContent.Url ->
            param.client.createBrowser(
                content.url,
                param.rendering,
                param.transparent,
//                param.requestContext,
            )

        is WebContent.Data ->
            param.client.createBrowserWithHtml(
                content.data,
                content.baseUrl ?: KCEFBrowser.BLANK_URI,
                param.rendering,
                param.transparent,
            )

        is WebContent.File ->
            param.client.createBrowserWithHtml(
                param.fileContent,
                KCEFBrowser.BLANK_URI,
                param.rendering,
                param.transparent,
            )

        else ->
            param.client.createBrowser(
                KCEFBrowser.BLANK_URI,
                param.rendering,
                param.transparent,
//                param.requestContext,
            )
    }
}
