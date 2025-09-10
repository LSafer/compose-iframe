package net.lsafer.compose.iframe.internal

import org.cef.CefSettings
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefDisplayHandler

internal class DisplayHandlerImpl(
    val onAddressChange: (String) -> Unit,
) : CefDisplayHandler {
    override fun onAddressChange(
        browser: CefBrowser?,
        frame: CefFrame?,
        url: String?,
    ) {
        onAddressChange(url ?: return)
    }

    override fun onTitleChange(
        browser: CefBrowser?,
        title: String?,
    ) {
    }

    override fun onFullscreenModeChange(
        p0: CefBrowser?,
        p1: Boolean,
    ) {
    }

    override fun onTooltip(
        browser: CefBrowser?,
        text: String?,
    ): Boolean {
        return false
    }

    override fun onStatusMessage(
        browser: CefBrowser?,
        value: String?,
    ) {
    }

    override fun onConsoleMessage(
        browser: CefBrowser?,
        level: CefSettings.LogSeverity?,
        message: String?,
        source: String?,
        line: Int,
    ): Boolean {
        return false
    }

    override fun onCursorChange(
        browser: CefBrowser?,
        cursorType: Int,
    ): Boolean {
        return false
    }
}
