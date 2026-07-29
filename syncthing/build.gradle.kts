// TODO, FIXME: Add other os-es and test windows and macos

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemOperations
import org.gradle.kotlin.dsl.support.serviceOf
import java.util.Properties

val goVersionShared = "1.26.3"

val setupGo: TaskProvider<Task> = tasks.register("setupGo") {
	description = "Set up Go inside this project to don't depend on system go version."
    val goVersion = goVersionShared

	val goInstallDir = layout.projectDirectory.dir("go/$goVersion").asFile
	val goBinDir = File(goInstallDir, "go/bin")

	// Gradle cache
	outputs.dir(goInstallDir)
	inputs.property("goVersion", goVersionShared)

	val fs = project.serviceOf<FileSystemOperations>()
	val archives = project.serviceOf<ArchiveOperations>()
	val projectDir = layout.projectDirectory

	doLast {

		val osName = System.getProperty("os.name").lowercase()
		val osArch = System.getProperty("os.arch").lowercase()
		val goOs = when {
			osName.contains("win") -> "windows" // FIXME: Untested
			osName.contains("mac") -> "darwin" // FIXME: Untested
			else -> "linux"
		}

		val goArch =
			if (osArch.contains("aarch64") || osArch.contains("arm64")) "arm64" else "amd64"
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

		println("Extracting Go from ${archive.relativeTo(projectDir.asFile)} into ${goInstallDir.relativeTo(projectDir.asFile)}")

		fs.copy {
			from(if (goExt == "zip") archives.zipTree(archive) else archives.tarTree(archive))
			into(goInstallDir)
		}
		println("Success!")
		archive.delete()

	}
}


// BUILD_TARGETS
data class GoTarget(
	val arch: String,
	val goArch: String,
	val jniDir: String,
	val ccTemplate: String
)

val buildTargets = listOf(
	GoTarget("arm", "arm", "armeabi-v7a", "armv7a-linux-androideabi%s-clang"),
	GoTarget("arm64", "arm64", "arm64-v8a", "aarch64-linux-android%s-clang"),
	GoTarget("x86", "386", "x86", "i686-linux-android%s-clang"),
	GoTarget("x86_64", "amd64", "x86_64", "x86_64-linux-android%s-clang")
)


// Git fetch tags
// TODO: Maybe don't depend on tags?
val fetchSyncthingTags = tasks.register("fetchSyncthingTags") {
	description = "Runs git fetch --tags in syncthing's git repo"
	val providerFactory: ProviderFactory = providers
	val repoDir = layout.projectDirectory.dir("src/github.com/syncthing/syncthing").asFile


    doLast {
		var output: ExecOutput? = null
		try {
			providerFactory.exec {
				workingDir = repoDir
				commandLine("git", "fetch", "--tags")
				isIgnoreExitValue = true // Don't crash if offline
			}
			output = providerFactory.exec {
				workingDir = repoDir
				commandLine("git", "tag")
				isIgnoreExitValue = true
			}
			// If I don't evaluate them here, the whole output won't run in the correct place
			output.standardOutput?.asText?.get()
			output.standardError?.asText?.get()
			output.result?.get()?.exitValue
		} catch (_: Exception) {
			logger.error("Git is not installed or not in PATH. Skipping tag fetch.")
		}
		println("stdout = ${output?.standardOutput?.asText?.get()}")
		println("stderr = ${output?.standardError?.asText?.get()}")
		println("return = ${output?.result?.get()?.exitValue}")

		if (output?.standardOutput?.asText?.get().isNullOrBlank()) {
			error("No Git tags were found!")
		}

	}
}


// stupid helper because exec didn't work
object ShellRunner {
	fun runShellCommand(vararg args: String, workDir: File, projectDir: File, env: Map<String, String>) {
		val pb = ProcessBuilder(args.toList())
		pb.directory(workDir)
		val contextEnv = pb.environment()
		contextEnv.putAll(System.getenv())
		contextEnv.putAll(env)

		val localGoBin = env["_GRADLE_GO_BIN_DIR"]
		if (localGoBin != null) {
			val pKey =
				if (System.getProperty("os.name").lowercase().contains("win")) "Path" else "PATH"
			contextEnv[pKey] = "$localGoBin${File.pathSeparator}${contextEnv[pKey]}"
		}

		pb.redirectErrorStream(true)
		println("Running command:\n\t${pb.command().map { val file = File(it).absoluteFile; if (file.exists()) file.relativeTo(projectDir) else it}.joinToString("\n\t")}")
		val process = pb.start()
		process.inputStream.bufferedReader().useLines { lines ->
			println("OUTPUT:")
			lines.forEach { println(it) }
		}
		val exitCode = process.waitFor()
		if (exitCode != 0) {
			error("Command failed with exit code $exitCode: ${args.joinToString(" ")}")
		}
	}
}

val buildNativeTasks = listOf("arm", "arm64", "x86", "x86_64").map { target ->

	tasks.register("buildNative_$target") {
		description = "Builds syncthing for $target architecture."
        dependsOn(setupGo, fetchSyncthingTags)
		val goVersion = goVersionShared


		// PLATFORM_DIRS
		val hostOsName = System.getProperty("os.name").lowercase()
		val ndkOs = when {
			hostOsName.contains("win") -> "windows-x86_64"
			hostOsName.contains("mac") -> "darwin-x86_64"
			else -> "linux-x86_64"
		}

		val goOs = when {
			hostOsName.contains("win") -> "windows"
			hostOsName.contains("mac") -> "darwin"
			else -> "linux"
		}
		val goBinaryName = if (goOs == "windows") "go.exe" else "go"

		val targetData = when (target) {
			"arm" -> listOf("arm", "armeabi-v7a", "armv7a-linux-androideabi%s-clang")
			"arm64" -> listOf("arm64", "arm64-v8a", "aarch64-linux-android%s-clang")
			"x86" -> listOf("386", "x86", "i686-linux-android%s-clang")
			"x86_64" -> listOf("amd64", "x86_64", "x86_64-linux-android%s-clang")
			else -> error("Unknown arch")
		}

		val goArch = targetData[0]
		val jniDir = targetData[1]
		val ccTemplate = targetData[2]

		// Paths
		val syncthingSrcDir =
			layout.projectDirectory.dir("src/github.com/syncthing/syncthing").asFile
		val pkgDir = layout.projectDirectory.dir("gobuild/go-packages/$goArch").asFile
		val jniOutDir = layout.projectDirectory.dir("../app/src/main/jniLibs/$jniDir").asFile
		val goBin = layout.projectDirectory.file("go/$goVersion/go/bin/$goBinaryName").asFile
		val goCache = layout.projectDirectory.dir("gobuild/go-cache").asFile

		// Gradle caching
		inputs.dir(syncthingSrcDir)
		outputs.dir(jniOutDir)

		// get_ndk_home():
		val localProperties = Properties().apply {
			val localPropertiesFile = rootProject.projectDir.resolve("local.properties")
			if (localPropertiesFile.exists()) {
				localPropertiesFile.inputStream().use { load(it) }
			}
		}

		val sdkDir = localProperties.getProperty("sdk.dir")
			?: System.getenv("ANDROID_HOME")
			?: ""

		val ndkVerson = libs.versions.ndk.get()
		val ndkDir = if (sdkDir.isNotEmpty()) "$sdkDir/ndk/$ndkVerson" else ""

		// get_min_sdk(project_dir):
		val minSdk = libs.versions.minSdk.get()

		val projectDir = layout.projectDirectory

		doLast {
			println("\n===========BUILDING FOR $target===========")
			println("Project dir: $projectDir")
			println("syncthingSrcDir = ${syncthingSrcDir.relativeTo(projectDir.asFile)}")
			println("pkgDir = ${pkgDir.relativeTo(projectDir.asFile)}")
			println("jniOutDir = ${jniOutDir.relativeTo(projectDir.asFile)}")
			println("goBin = ${goBin.relativeTo(projectDir.asFile)}")
			println("goCache = ${goCache.relativeTo(projectDir.asFile)}")


			val ndkBinDir = File("$ndkDir/toolchains/llvm/prebuilt/$ndkOs/bin")
			val ccPath = File(ndkBinDir, ccTemplate.format(minSdk)).absolutePath

			// Ensure build directories exist
			pkgDir.mkdirs()
			goCache.mkdirs()

			// Environment for Host Tools
			val hostEnv = mapOf(
				"GOROOT" to goBin.parentFile.parentFile.absolutePath,
				"GO111MODULE" to "on",
				"_GRADLE_GO_BIN_DIR" to goBin.parentFile.absolutePath
			)

			// Environment for Cross-Compiling
			val targetEnv = mutableMapOf<String, String>().apply {
				putAll(hostEnv)
				put("CGO_ENABLED", "1")
				put("GOCACHE", goCache.absolutePath)

				// this is very important for building the android variant.
				put("EXTRA_LDFLAGS", "-checklinkname=0")
			}.toMap()

			val goExe = goBin.absolutePath

			ShellRunner.runShellCommand(goExe, "version", workDir = syncthingSrcDir, projectDir = projectDir.asFile, env = hostEnv)

			ShellRunner.runShellCommand(
				goExe,
				"run",
				"build.go",
				"version",
				workDir = syncthingSrcDir,
				projectDir = projectDir.asFile,
				env = hostEnv
			)

			val artifact = File(temporaryDir, "bin")
			ShellRunner.runShellCommand(
				goExe,
				"run",
				"build.go",
				"-goos", "android",
				"-goarch", goArch,
				"-cc", ccPath,
				"-pkgdir", pkgDir.absolutePath,
				"-no-upgrade",
				"-build-out", artifact.absolutePath,
				"build",
				workDir = syncthingSrcDir,
				projectDir = projectDir.asFile,
				env = targetEnv
			)

			// Move output
			if (artifact.exists()) {
				jniOutDir.mkdirs()
				println(
					"Moving file ${artifact.path} -> ${jniOutDir.path}${
						if (hostOsName.contains(
								"win"
							)
						) "\\" else "/"
					}libsyncthing.so"
				)
				artifact.copyTo(File(jniOutDir, "libsyncthing.so"), overwrite = true)
				artifact.delete()
				println("Finished build for $target")
			} else {
				error("Build produced no artifact at $artifact")
			}
		}
	}
}

// Don't run buildNative tasks concurrently, as go build system already uses concurrency.
buildNativeTasks.zipWithNext().forEach { (previousTask, currentTask) ->
	currentTask.configure {
		mustRunAfter(previousTask)
	}
}


tasks.register("buildNative") {
	description = "This is a stub task so buildNativeTasks can be a list and reuse code."
    dependsOn(buildNativeTasks)
	doLast {
		println("All builds finished")
	}
}

/**
 * Use separate task instead of standard clean(), so these folders aren't deleted by `gradle clean`.
 */
tasks.register<Delete>("cleanNative") {
	description = "Clean syncthing build files."
    delete("$projectDir/../app/src/main/jniLibs/")
	delete("gobuild")
}