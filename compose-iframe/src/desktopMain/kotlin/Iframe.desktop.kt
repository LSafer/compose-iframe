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
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

@Composable
actual fun Iframe(state: IframeState, modifier: Modifier) {
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

    CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
        state.getOrCreateBrowser().let { browser ->
            SwingPanel(
                background = Color.Transparent,
                factory = { browser.uiComponent },
                modifier = modifier,
            )
        }
    }
}
