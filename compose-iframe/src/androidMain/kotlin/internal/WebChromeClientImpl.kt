package net.lsafer.compose.iframe.internal

import android.content.Context
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class WebChromeClientImpl(
    val context: Context,
    val coroutineScope: CoroutineScope,
    val onLoadStarted: () -> Unit,
    val onLoadEnd: () -> Unit,
    val onAddressChange: (String) -> Unit,
) : WebChromeClient() {
    override fun onReceivedTitle(view: WebView, title: String?) {
        onAddressChange(view.url ?: return)
    }

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        if (newProgress < 100)
            onLoadStarted()
        else
            onLoadEnd()
        onAddressChange(view.url ?: return)
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: FileChooserParams
    ): Boolean {
        val extensions = fileChooserParams.acceptTypes?.map {
            it.substringAfterLast('/').removePrefix(".")
        }.orEmpty()

        when (fileChooserParams.mode) {
            FileChooserParams.MODE_OPEN -> coroutineScope.launch {
                val file = FileKit.openFilePicker(
                    type = FileKitType.File(extensions),
                    mode = FileKitMode.Single,
                    title = fileChooserParams.title?.toString(),
                )

                if (file == null) {
                    filePathCallback.onReceiveValue(null)
                    return@launch
                }

                filePathCallback.onReceiveValue(arrayOf(file.toAndroidUri("")))
            }

            FileChooserParams.MODE_OPEN_MULTIPLE -> coroutineScope.launch {
                val files = FileKit.openFilePicker(
                    type = FileKitType.File(extensions),
                    mode = FileKitMode.Multiple(),
                    title = fileChooserParams.title?.toString(),
                )

                if (files == null) {
                    filePathCallback.onReceiveValue(null)
                    return@launch
                }

                filePathCallback.onReceiveValue(files.map { it.toAndroidUri("") }.toTypedArray())
            }

            FileChooserParams.MODE_SAVE -> coroutineScope.launch {
                val result = FileKit.openFileSaver(
                    suggestedName = fileChooserParams.filenameHint.orEmpty(),
                    extension = fileChooserParams.filenameHint?.substringAfterLast("."),
                )

                if (result == null) {
                    filePathCallback.onReceiveValue(null)
                    return@launch
                }

                filePathCallback.onReceiveValue(arrayOf(result.toAndroidUri("")))
            }

            2 /*FileChooserParams.MODE_OPEN_FOLDER*/ -> coroutineScope.launch {
                val result = FileKit.openDirectoryPicker(
                    title = fileChooserParams.title?.toString(),
                )

                if (result == null) {
                    filePathCallback.onReceiveValue(null)
                    return@launch
                }

                filePathCallback.onReceiveValue(arrayOf(result.toAndroidUri("")))
            }

            else -> return false
        }

        return true
    }
}
