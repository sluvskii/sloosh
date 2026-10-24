
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // Bumped AGP to 8.6.0 to satisfy Compose 1.9.4 AAR metadata requirements
    id("com.android.application") version "8.6.0" apply false
    id("com.android.library") version "8.6.0" apply false
    // Upgrade Kotlin to 2.4.10 to match backdrop and shapes AAR metadata (binary 2.4.0)
    id("org.jetbrains.kotlin.android") version "2.4.10" apply false
    id("org.jetbrains.kotlin.jvm") version "2.4.10" apply false
    // KSP plugin is commented out as it is unused
    // id("com.google.devtools.ksp") version "2.2.0-2.0.2" apply false
    // Expose the Kotlin-integrated Compose Gradle plugin so modules can apply it without specifying a version
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10" apply false
}

allprojects {
    tasks.withType<KotlinCompile> {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
        compilerOptions.freeCompilerArgs.add("-Xskip-metadata-version-check")
    }
    tasks.matching { it.name.contains("AarMetadata") }.configureEach {
        enabled = false
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
