import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("kotlin-parcelize")
}

android {
    namespace = "com.slooshfilm.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.slooshfilm.app"
        minSdk = 33
        targetSdk = 34

        val buildNum = System.getenv("BUILD_NUMBER")?.toIntOrNull()
            ?: project.findProperty("versionCode")?.toString()?.toIntOrNull()
            ?: 22
        // Базовый оффсет 20000 гарантирует совместимость с AppUpdater (2.0.x -> 20000 + x)
        // и защищает от ошибки INSTALL_FAILED_VERSION_DOWNGRADE при обновлении поверх старых версий (versionCode 20-22).
        versionCode = 20000 + buildNum
        versionName = System.getenv("VERSION_NAME")
            ?: project.findProperty("versionName")?.toString()
            ?: "2.0.$buildNum"

        buildConfigField("String", "GITHUB_OWNER", "\"sluvskii\"")
        buildConfigField("String", "GITHUB_REPO", "\"sloosh\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        // Release keystore lives in key/sloosh-release.keystore; credentials are read from
        // local.properties (gitignored). Falls back to the debug key when the entries are
        // missing so the project keeps building on fresh clones — a debug-signed release
        // is still installable, it just can't update over release-signed installs.
        val props = rootProject.file("local.properties")
        val signingProps = if (props.exists()) {
            Properties().apply { props.inputStream().use { load(it) } }
        } else null
        val storeFile = signingProps?.getProperty("SLOOSH_STORE_FILE")
        val storePass = signingProps?.getProperty("SLOOSH_STORE_PASSWORD")
        val keyAlias = signingProps?.getProperty("SLOOSH_KEY_ALIAS")
        val keyPass = signingProps?.getProperty("SLOOSH_KEY_PASSWORD")
        if (!storeFile.isNullOrBlank() && !storePass.isNullOrBlank() && !keyAlias.isNullOrBlank()) {
            create("release") {
                this.storeFile = rootProject.file(storeFile)
                this.storePassword = storePass
                this.keyAlias = keyAlias
                this.keyPassword = keyPass ?: storePass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xskip-metadata-version-check")
    }
}

dependencies {
    // AndroidX Core, AppCompat & Material
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Jetpack Compose
    implementation("androidx.compose.ui:ui:1.7.0")
    implementation("androidx.compose.ui:ui-graphics:1.7.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.7.0")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Image loading with Coil
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Palette Color Extraction
    implementation("androidx.palette:palette-ktx:1.0.0")

    // Media3 ExoPlayer
    implementation("androidx.media3:media3-common:1.3.1")
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")
    implementation("androidx.media3:media3-session:1.3.1")

    // WebKit
    implementation("androidx.webkit:webkit:1.11.0")

    // Networking & Serialization
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // Room removed: storage is SharedPreferences+Gson; no annotations were processed
    // (KSP was disabled), so the runtime jars were dead weight.

    // iOS Shapes (Backdrop engine is now fully in-tree in com.slooshfilm.app.ui.backdrop)
    implementation("io.github.kyant0:shapes:1.2.1")

    // Google Play Services Auth for Native System Bottom Sheet Sign-In
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // Debugging & Tooling
    debugImplementation("androidx.compose.ui:ui-tooling:1.7.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.7.0")
}

tasks.matching { it.name.contains("AarMetadata") }.configureEach {
    enabled = false
}
