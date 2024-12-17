package net.lsafer.compose.iframe

import androidx.compose.runtime.*
import dev.datlag.kcef.KCEF
import dev.datlag.kcef.KCEFBuilder
import dev.datlag.kcef.KCEFBuilder.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

val LocalJcefStartupState = compositionLocalOf<JcefStartupState> {
    error("LocalJcefStartupState is not present")
}

class JcefStartupState {
    var isInitialized by mutableStateOf(false)
        internal set
    var progress by mutableStateOf(0f) // [0f..100f]
        internal set
    internal val _errors = mutableStateListOf<Throwable>()
    val errors: List<Throwable> = _errors
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
                onError = { if (it != null) startup._errors += it },
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
