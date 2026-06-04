// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    // IMPORTANT NOTE: Kotlin 2.3.21 uses the Compose Compiler Gradle plugin; do not add legacy composeOptions.kotlinCompilerExtensionVersion.
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
