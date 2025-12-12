plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.0"
}

android {
    namespace = "com.slooshfilm.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.slooshfilm.app"
        minSdk = 33
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "GITHUB_OWNER", "\"your_github_owner\"")
        buildConfigField("String", "GITHUB_REPO", "\"your_repo\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
        compose = true
    }
    // Compose plugin supplies the compiler integration for Kotlin 2.2
    // (keep this block commented out if using plugin-managed compose compiler)
    // composeOptions {
    //     kotlinCompilerExtensionVersion = "2.2.0"
    // }
}



dependencies {
    // Core & UI
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("androidx.fragment:fragment-ktx:1.7.0")
    implementation("com.google.android.flexbox:flexbox:3.0.0")

    // Compose - align with AndroidLiquidGlass/backdrop AAR which targets Compose 1.9.4
    implementation("androidx.compose.ui:ui:1.9.4")
    implementation("androidx.compose.ui:ui-graphics:1.9.4")
    implementation("androidx.compose.ui:ui-tooling-preview:1.9.4")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.9.4")
    debugImplementation("androidx.compose.ui:ui-tooling:1.9.4")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.9.4")
    implementation("androidx.compose.material3:material3:1.1.0")
    implementation("androidx.compose.material:material-icons-extended:1.6.7")
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Player
    implementation("androidx.media3:media3-common:1.8.0")
    implementation("androidx.media3:media3-exoplayer:1.8.0")
    implementation("androidx.media3:media3-ui:1.8.0")
    implementation("androidx.media3:media3-session:1.8.0")

    // Room
    implementation("androidx.room:room-runtime:2.7.0")
    implementation("androidx.room:room-ktx:2.7.0")
    // Use KSP for Room annotation processing (KAPT -> KSP)
    ksp("androidx.room:room-compiler:2.7.0")

    // Networking & Parsing
    implementation(project(":library"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.jsoup:jsoup:1.16.1")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("jp.wasabeef:glide-transformations:4.3.0")
    implementation("com.facebook.shimmer:shimmer:0.5.0")

    // Testing
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
