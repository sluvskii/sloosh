
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // Bumped AGP to 8.6.0 to satisfy Compose 1.9.4 AAR metadata requirements
    id("com.android.application") version "8.6.0" apply false
    id("com.android.library") version "8.6.0" apply false
    // Attempt: upgrade Kotlin to 2.2.0 to match backdrop AAR metadata (binary 2.2.0)
    // NOTE: this may require further updates (Compose compiler, AGP). We'll iterate on build errors.
    id("org.jetbrains.kotlin.android") version "2.2.0" apply false
    id("org.jetbrains.kotlin.jvm") version "2.2.0" apply false
    // KSP plugin version usually mirrors Kotlin; try a matching KSP release - adjust if resolution fails
    // KSP plugin removed from root for now to avoid unresolved plugin errors while we use KAPT instead.
    // If you later want KSP, we'll add a matching KSP release here.
    // Expose the Kotlin-integrated Compose Gradle plugin so modules can apply it without specifying a version
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.0" apply false
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
    // Add KSP plugin matching Kotlin 2.2.0 (use published KSP coordinates)
    id("com.google.devtools.ksp") version "2.2.0-2.0.2" apply false
    id("com.google.gms.google-services") version "4.4.1" apply false
}

allprojects {
    tasks.withType<KotlinCompile> {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_1_8)
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
