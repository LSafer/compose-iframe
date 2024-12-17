import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `maven-publish`
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose)
}

kotlin {
    jvm("desktop")

    js {
        browser()
        useEsModules()
    }

    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    sourceSets {
        val kevinnzouMain by creating
        val desktopMain by getting

        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.material3)
        }

        kevinnzouMain.dependsOn(commonMain.get())
        kevinnzouMain.dependencies {
            implementation(libs.kevinnzou.compose.webview)
        }

        desktopMain.dependsOn(kevinnzouMain)
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)

            implementation(libs.jcef)
            implementation(libs.kcef)
        }

        androidMain { dependsOn(kevinnzouMain) }
        androidMain.dependencies {
            implementation(compose.preview)
        }
    }
}

android {
    namespace = "net.lsafer.compose.iframe"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    dependencies {
        debugImplementation(compose.uiTooling)
    }
}

afterEvaluate {
    publishing {
        publications {
            // Creates a Maven publication called "release".
            create<MavenPublication>("release") {
                // Applies the component for the release build variant.
                from(components["kotlin"])

                // You can then customize attributes of the publication as shown below.
                groupId = "net.lsafer"
                artifactId = "compose-iframe"
                version = project.version.toString()
            }
        }
    }
}
