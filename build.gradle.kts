// Top-level build file where you can add configuration options common to all sub-projects/modules.


buildscript {
    extra.apply {
        // Cannot be called "ndkVersion" as that leads to naming collision
        // Changes to this value must be reflected in `./docker/Dockerfile`
        //set("ndkVersionShared", "29.0.13113456") // not needed if built in docker

    }

    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }

}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose.compiler) apply false
    alias(libs.plugins.androidx.baselineprofile) apply false
    alias(libs.plugins.benManes.versions) apply false
}


tasks.register<Delete>("clean") {
    delete(getLayout().buildDirectory)
    delete("${getLayout().projectDirectory}/app/src/main/jniLibs")
}
