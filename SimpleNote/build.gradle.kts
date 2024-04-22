buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        val navVersion = "2.5.2" // Example version, ensure this matches the version used in app level
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:$navVersion")
        // Other classpath dependencies...
    }
}


// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false
    id("com.android.library") version "8.3.1" apply false
    id("com.google.gms.google-services") version "4.4.0" apply false
}