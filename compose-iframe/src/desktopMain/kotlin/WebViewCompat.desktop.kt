package net.lsafer.compose.iframe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.multiplatform.webview.jsbridge.WebViewJsBridge
import com.multiplatform.webview.web.*
import dev.datlag.kcef.KCEF
import dev.datlag.kcef.KCEFBrowser
import dev.datlag.kcef.KCEFBuilder
import dev.datlag.kcef.KCEFBuilder.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class JcefStartupState {
    var isInitialized by mutableStateOf(false)
        internal set
    var progress by mutableStateOf(0f) // [0f..100f]
        internal set
    internal val errorsList = mutableStateListOf<Throwable>()
    val errors: List<Throwable> = errorsList
}

private val LocalJcefStartupState = compositionLocalOf<JcefStartupState> {
    error("LocalJcefStartupState is not present")
}

@Composable
fun JcefStartupScope(
    bundle: File,
    download: KCEFBuilder.Download.Builder.() -> Unit = {
        github { release("jbr-release-17.0.10b1087.23") }
    },
    settings: Settings.() -> Unit = {},
    onRestartRequired: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    // see https://github.com/DatL4g/KCEF/blob/master/COMPOSE.md

    val startup = remember { JcefStartupState() }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            KCEF.init(
                builder = {
                    installDir(bundle)

                    progress {
                        onDownloading {
                            // this handles the issue were the download
                            // starts at 100% then goes down back to 0
                            if (startup.progress == 0f && it == 100f)
                                return@onDownloading

                            startup.progress = it.coerceIn(0f, 100f)
                        }
                        onInitialized {
                            startup.isInitialized = true
                        }
                    }
                    download(download)
                    settings(settings)
                },
                onError = { if (it != null) startup.errorsList += it },
                onRestartRequired = { onRestartRequired() },
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            KCEF.disposeBlocking()
        }
    }

    CompositionLocalProvider(
        LocalJcefStartupState provides startup,
    ) {
        content()
    }
}

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
