// Top-level build file where you can add configuration options common to all sub-projects/modules.


buildscript {
    extra.apply {
        // Cannot be called "ndkVersion" as that leads to naming collision
        // Changes to this value must be reflected in `./docker/Dockerfile`
        //set("ndkVersionShared", "29.0.13113456") // not needed if built in docker

    }
    val benchmarkVersion = gradle.extra.get("benchmarkVersion") as String

    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    dependencies {
        classpath("androidx.benchmark:benchmark-baseline-profile-gradle-plugin:$benchmarkVersion")
        classpath("com.android.tools.build:gradle:9.3.0")
        classpath("com.github.ben-manes:gradle-versions-plugin:0.36.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.21")
        classpath("org.jetbrains.kotlin:compose-compiler-gradle-plugin:2.2.21")
    }
}

extra.set("benchmarkVersion", gradle.extra.get("benchmarkVersion") as String)

tasks.register<Delete>("clean") {
    delete(getLayout().buildDirectory)
    delete("${getLayout().projectDirectory}/app/src/main/jniLibs")
}
