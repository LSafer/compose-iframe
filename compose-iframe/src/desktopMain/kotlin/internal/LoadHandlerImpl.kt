package net.lsafer.compose.iframe.internal

import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandler
import org.cef.network.CefRequest

internal class LoadHandlerImpl(
    val onLoadingStart: () -> Unit,
    val onLoadingEnd: () -> Unit,
) : CefLoadHandler {
    override fun onLoadingStateChange(
        browser: CefBrowser?,
        isLoading: Boolean,
        canGoBack: Boolean,
        canGoForward: Boolean,
    ) {
        if (isLoading) {
            onLoadingStart()
        } else {
            onLoadingEnd()
        }
    }

    override fun onLoadStart(
        browser: CefBrowser?,
        frame: CefFrame?,
        transitionType: CefRequest.TransitionType?,
    ) {
        onLoadingStart()
    }

    override fun onLoadEnd(
        browser: CefBrowser?,
        frame: CefFrame?,
        httpStatusCode: Int,
    ) {
        onLoadingEnd()
    }

    override fun onLoadError(
        browser: CefBrowser?,
        frame: CefFrame?,
        errorCode: CefLoadHandler.ErrorCode?,
        errorText: String?,
        failedUrl: String?,
    ) {
        onLoadingEnd()
    }
}
