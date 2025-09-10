package net.lsafer.compose.iframe.internal

import android.graphics.Bitmap
import android.webkit.WebView
import android.webkit.WebViewClient

internal class WebViewClientImpl(
    val onLoadStarted: () -> Unit,
    val onLoadEnd: () -> Unit,
    val onAddressChange: (String) -> Unit,
) : WebViewClient() {
    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        onLoadStarted()
        if (url != null) onAddressChange(url)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        onLoadEnd()
        if (url != null) onAddressChange(url)
    }
}
