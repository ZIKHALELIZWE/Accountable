// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("com.google.devtools.ksp") version "2.2.20-2.0.3" apply false
    kotlin("plugin.serialization") version "2.0.21" apply false
}
buildscript{
    repositories{
        google()
    }
    dependencies {
        classpath(libs.androidx.androidx.navigation.safeargs.gradle.plugin)
    }
}