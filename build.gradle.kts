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
    dependencies {
        classpath("com.android.tools.build:gradle:9.0.0")
        classpath("com.github.ben-manes:gradle-versions-plugin:0.36.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.21")
        classpath("org.jetbrains.kotlin:compose-compiler-gradle-plugin:2.2.21")

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

tasks.register<Delete>("clean") {
    delete(getLayout().buildDirectory)
    delete("${getLayout().projectDirectory}/app/src/main/jniLibs")
}
