package net.lsafer.compose.iframe

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
actual fun Iframe(state: IframeState, modifier: Modifier) {
    BackHandler(state.canGoBack) {
        state.webView.goBack()
    }

    BoxWithConstraints(modifier) {
        val width = if (constraints.hasFixedWidth) {
            ViewGroup.LayoutParams.MATCH_PARENT
        } else {
            ViewGroup.LayoutParams.WRAP_CONTENT
        }
        val height = if (constraints.hasFixedHeight) {
            ViewGroup.LayoutParams.MATCH_PARENT
        } else {
            ViewGroup.LayoutParams.WRAP_CONTENT
        }

        val layoutParams = FrameLayout.LayoutParams(width, height)

        AndroidView(
            factory = {
                state.getOrCreateWebView(it).also {
                    it.layoutParams = layoutParams
                }
            },
        )
    }
}
