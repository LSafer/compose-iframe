# Iframe component for compose-multiplatform (desktop + android) [![](https://jitpack.io/v/net.lsafer/compose-iframe.svg)](https://jitpack.io/#net.lsafer/compose-iframe)

This is a wrapper for [KevinnZou](https://github.com/KevinnZou/compose-webview-multiplatform)'s
library with primary focus of providing an `iframe` like experience.

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
