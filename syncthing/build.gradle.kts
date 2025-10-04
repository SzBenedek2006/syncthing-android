import ru.vyarus.gradle.plugin.python.task.PythonTask

plugins {
    id("ru.vyarus.use-python") version "3.0.0"
}
/*
tasks.register<PythonTask>("buildNative") {
    val ndkVersionShared = rootProject.extra.get("ndkVersionShared")
    environment("NDK_VERSION", "$ndkVersionShared")
    inputs.dir("$projectDir/src/")
    outputs.dir("$projectDir/../app/src/main/jniLibs/")
    command = "-u ./build-syncthing.py"
}
*/

tasks.register<Exec>("buildNative") {
    val mountVolume = "$rootDir:/mnt"
    val scriptPath = "syncthing/build-syncthing.py"

    inputs.dir("$rootDir/syncthing")
    outputs.dir("$rootDir/app/src/main/jniLibs")



    commandLine = listOf(
        "podman", "run", "--rm",
        "-v", mountVolume,
        "-e", "EXTRA_LDFLAGS=-checklinkname=0",
        "syncthing-android-builder",
        "python3", scriptPath // The path is now correct relative to the mount point
    )
}




/**
 * Use separate task instead of standard clean(), so these folders aren't deleted by `gradle clean`.
 */
tasks.register<Delete>("cleanNative") {
    delete("$projectDir/../app/src/main/jniLibs/")
    delete("gobuild")
}
