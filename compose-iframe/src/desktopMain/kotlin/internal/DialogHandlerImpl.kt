package net.lsafer.compose.iframe.internal

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.dialogs.openFileSaver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.cef.browser.CefBrowser
import org.cef.callback.CefFileDialogCallback
import org.cef.handler.CefDialogHandler
import org.cef.handler.CefDialogHandler.FileDialogMode.*
import java.util.*

class DialogHandlerImpl(val coroutineScope: CoroutineScope) : CefDialogHandler {
    override fun onFileDialog(
        browser: CefBrowser?,
        mode: CefDialogHandler.FileDialogMode?,
        title: String?,
        defaultFilePath: String?,
        acceptFilters: Vector<String>?,
        callback: CefFileDialogCallback?
    ): Boolean {
        val extensions = acceptFilters?.map {
            it.substringAfterLast('/').removePrefix(".")
        }.orEmpty()

        when (mode) {
            FILE_DIALOG_OPEN -> coroutineScope.launch {
                val file = FileKit.openFilePicker(
                    type = FileKitType.File(extensions),
                    mode = FileKitMode.Single,
                    title = title,
                )

                if (file == null) {
                    callback?.Cancel()
                    return@launch
                }

                val result = Vector<String>()
                result.add(file.absolutePath())
                callback?.Continue(result)
            }

            FILE_DIALOG_OPEN_MULTIPLE -> coroutineScope.launch {
                val files = FileKit.openFilePicker(
                    type = FileKitType.File(extensions),
                    mode = FileKitMode.Multiple(),
                    title = title,
                )

                if (files == null) {
                    callback?.Cancel()
                    return@launch
                }

                val result = Vector<String>()
                files.forEach { result.add(it.absolutePath()) }
                callback?.Continue(result)
            }

            FILE_DIALOG_SAVE -> coroutineScope.launch {
                val file = FileKit.openFileSaver(
                    suggestedName = defaultFilePath?.substringAfterLast("/").orEmpty(),
                    extension = defaultFilePath?.substringAfterLast("."),
                )

                if (file == null) {
                    callback?.Cancel()
                    return@launch
                }

                val result = Vector<String>()
                result.add(file.absolutePath())
                callback?.Continue(result)
            }

            else -> return false
        }

        return true
    }
}
