## Desktop Setup

### In settings.gradle.kts

Add the jogmap repo:

```kotlin
dependencyResolutionManagement {
    repositories {
        // ...
        maven("https://jogamp.org/deployment/maven")
    }
}
```

### In build.gradle.kts

```kotlin
compose.desktop {
    application {
        // ...

        jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
        jvmArgs("--add-opens", "java.desktop/java.awt.peer=ALL-UNNAMED") // recommended but not necessary

        if (System.getProperty("os.name").contains("Mac")) {
            jvmArgs("--add-opens", "java.desktop/sun.lwawt=ALL-UNNAMED")
            jvmArgs("--add-opens", "java.desktop/sun.lwawt.macosx=ALL-UNNAMED")
        }
    }
}
```

### In Code

You need to wrap the iframe component with this:

```kotlin
@Composable
fun App() { // <-- top most component
    JcefStartupScope(
        bundle = File("./kcef-bundle"), // <-- were to store the downloaded cef bundle
        download = {
            // configure from where and what version to download
            github { release("jbr-release-21.0.8b1038.71") }
        },
        settings = {
            // cef configuration
            cachePath = "./cache" // <-- necessary to persist cookies and localStorage
            logSeverity = KCEFBuilder.Settings.LogSeverity.Info
            windowlessRenderingEnabled = true
        },
        onRestartRequired = {
            // rare to occur. prompt the user to restart the application when it happens
        },
    ) {
        // The application content. (we use the global instance, yet it is better to only use webview here)
    }
}
```


