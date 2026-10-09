// ROOT build.gradle.kts
// Aura lesson AB-11: Hilt plugin must be declared in root AND app build.gradle.kts
plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("com.google.dagger.hilt.android") version "2.50" apply false
    id("org.jetbrains.kotlin.kapt") version "1.9.22" apply false
}
