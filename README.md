# Iframe component for compose-multiplatform (desktop + android + JS + WASM) [![](https://jitpack.io/v/net.lsafer/compose-iframe.svg)](https://jitpack.io/#net.lsafer/compose-iframe)

This is a wrapper for [KevinnZou](https://github.com/KevinnZou/compose-webview-multiplatform)'s
library with primary focus of providing an `iframe` like experience.

On javascript, this implementation creates an actual `iframe`
element using `document.createElement("iframe")` and position it
at the correct place using `Modifier.onGloballyPositioned { ... }`.
However, this approach does not support thigs rendered above the
iframe.

### Install

The main way of installing this library is
using `jitpack.io`

```kts
repositories {
    // ...
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    // Replace TAG with the desired version
    implementation("net.lsafer.compose-iframe:compose-iframe:TAG")
}
```

### Usage

This is an example of using a dummy iframe I created and hosted by GitHub Pages:

```kotlin
@Serializable
data class ExampleEventData(
    val type: String,
    val value: String,
)

@Composable
fun Component() {
    val url = "https://lsafer-meemer.github.io/iframe-copy-cat/iframe.html"
    val state = rememberIframeState(url)

    LaunchedEffect(Unit) {
        launch {
            for (event in state.incoming) {
                val data: ExampleEventData = try {
                    Json.decodeFromJsonElement(event.data)
                } catch (e: Throwable) {
                    continue
                }

                if (data.type != "demoIn") continue

                println("Gotten: ${data.value}")
            }
        }
        launch {
            while (true) repeat(1_000) {
                delay(2.seconds)

                val data = ExampleEventData(
                    type = "demoOut",
                    value = "Hello World: $it"
                )

                val event = IframeEvent(
                    Json.encodeToJsonElement(data),
                )

                state.outgoing.send(event)
            }
        }
    }

    IframeCompat(state, Modifier.fillMaxSize())
}
```

### Setup (for desktop)

```kotlin
@Composable
fun App() { // <-- top most component
    JcefStartupScope(
        bundle = File("./kcef-bundle"), // <-- were to store the downloaded cef bundle
        download = {
            // configure from where and what version to download
        },
        settings = {
            // cef configuration
            cachePath = "./cache" // <-- necessary to persist cookies and localStorage
        },
        onRestartRequired = {
            // rare to occur. prompt the user to restart the application when it happens
        },
    ) {
        // The application content. (we use the global instance, yet it is better to only use webview here)
    }
}
```
