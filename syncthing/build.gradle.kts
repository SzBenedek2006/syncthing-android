// TODO, FIXME: Add other os-es and test windows and macos

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.Locale

//import ru.vyarus.gradle.plugin.python.task.PythonTask

//plugins {
//    id("ru.vyarus.use-python") version "4.1.0"
//}
/*
tasks.register<PythonTask>("buildNative") {
    val ndkVersionShared = rootProject.extra.get("ndkVersionShared")
    environment("NDK_VERSION", "$ndkVersionShared")
    inputs.dir("$projectDir/src/")
    outputs.dir("$projectDir/../app/src/main/jniLibs/")
    command = "-u ./build-syncthing.py"
}
*/



val setupGo: TaskProvider<Task> = tasks.register("setupGo") {
    val goVersion = "1.26.1"

    val goInstallDir = layout.projectDirectory.dir(".gradle/go/$goVersion").asFile
    val goBinDir = File(goInstallDir, "go/bin")
    // Gradle will cache this task as long as the directory exists
    outputs.dir(goInstallDir)
    outputs.upToDateWhen { goBinDir.exists() }


    doLast {

        val osName = System.getProperty("os.name").lowercase()
        val osArch = System.getProperty("os.arch").lowercase()
        val goOs = when {
            osName.contains("win") -> "windows" // FIXME: Untested
            osName.contains("mac") -> "darwin" // FIXME: Untested
            else -> "linux"
        }

        val goArch = if (osArch.contains("aarch64") || osArch.contains("arm64")) "arm64" else "amd64"
        val goExt = if (goOs == "windows") "zip" else "tar.gz"

        val goUrl = "https://go.dev/dl/go$goVersion.$goOs-$goArch.$goExt"
        val archive = temporaryDir.resolve("go.$goExt")

        println("Downloading Go from $goUrl...")

        val client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()

        val request = HttpRequest.newBuilder()
            .uri(URI.create(goUrl))
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofFile(archive.toPath()))
        if (response.statusCode() != 200) {
            archive.delete()
            error("Failed to download Go! Server returned HTTP ${response.statusCode()}")
        }

        println("Extracting Go from $archive into $goInstallDir")

        copy {
            from(if (goExt == "zip") zipTree(archive) else tarTree(archive))
            into(goInstallDir)
        }
        println("Success!")
        archive.delete()

    }
}
tasks.register<Exec>("buildNative") {
    dependsOn(setupGo) // Currently not needed, but it will soon.

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
