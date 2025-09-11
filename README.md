# Iframe component for compose-multiplatform (desktop + android + JS + WASM) [![](https://jitpack.io/v/net.lsafer/compose-iframe.svg)](https://jitpack.io/#net.lsafer/compose-iframe)

On desktop, this implementation uses `kcef` in for rendering
and `FileKit` for file picker integration. [How to setup for desktop](./SETUP_DESKTOP.md)

On android, this implementation uses `androidx.webkit` for
rendering and `FileKit` for file picker integration. [How to setup for Android](./SETUP_ANDROID.md)

On browser, this implementation creates an actual `iframe`
element using `document.createElement("iframe")` and position it
at the correct place using `Modifier.onGloballyPositioned { ... }`.
However, this approach does not support thigs rendered above the
iframe.

> Inspiration and code stolen from (not dependency): https://github.com/KevinnZou/compose-webview-multiplatform

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
    val coroutineScope = rememberCoroutineScope()
    val iframe = remember { IframeState(coroutineScope) }

    LaunchedEffect(Unit) {
        iframe.src = "https://lsafer-meemer.github.io/iframe-copy-cat/iframe.html"
    }

    LaunchedEffect(Unit) {
        launch {
            for (event in iframe.incoming) {
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

                val event = IframeOutgoingEvent(
                    Json.encodeToJsonElement(data),
                )

                iframe.outgoing.send(event)
            }
        }
    }

    Iframe(iframe, Modifier.fillMaxSize())
}
```
