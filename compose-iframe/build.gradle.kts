import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `maven-publish`
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose)
}

group = "net.lsafer.compose-iframe"
version = "local_snapshot"

kotlin {
    applyDefaultHierarchyTemplate()

    androidTarget {
        publishLibraryVariants("release")
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    jvmToolchain(22)
    jvm("desktop")
    js { browser() }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs { browser() }
    sourceSets {
        val commonMain by getting

        val webMain by creating
        val jsMain by getting
        val wasmJsMain by getting

        webMain.dependsOn(commonMain)
        jsMain.dependsOn(webMain)
        wasmJsMain.dependsOn(webMain)
    }
    sourceSets.commonMain.dependencies {
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kotlinx.datetime)

        implementation(compose.runtime)
        implementation(compose.foundation)
        implementation(compose.ui)
        implementation(compose.material3)
    }
    sourceSets.androidMain.dependencies {
        implementation(compose.preview)
        implementation(libs.androidx.activity.compose)
        implementation(libs.androidx.webkit)
        implementation(libs.filekit.core)
        implementation(libs.filekit.dialogs)
    }
    sourceSets.named("desktopMain").dependencies {
        implementation(compose.desktop.currentOs)

        implementation(libs.jcef)
        implementation(libs.kcef)
    }
    sourceSets.named("webMain").dependencies {
        implementation(libs.kotlinx.browser)
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
