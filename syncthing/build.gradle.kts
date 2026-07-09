// TODO, FIXME: Add other os-es and test windows and macos

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.Locale
import javax.inject.Inject
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemOperations
import org.gradle.kotlin.dsl.support.serviceOf

val goVersionShared = "1.26.3"

val setupGo: TaskProvider<Task> = tasks.register("setupGo") {
	val goVersion = goVersionShared

	val goInstallDir = layout.projectDirectory.dir("go/$goVersion").asFile
	val goBinDir = File(goInstallDir, "go/bin")

	// Gradle cache
	outputs.dir(goInstallDir)
	inputs.property("goVersion", goVersionShared)

	val fs = project.serviceOf<FileSystemOperations>()
	val archives = project.serviceOf<ArchiveOperations>()

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

		println("Extracting Go from $archive into $goInstallDir")

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
val fetchSyncthingTags = tasks.register<Exec>("fetchSyncthingTags") {
	workingDir = layout.projectDirectory.dir("src/github.com/syncthing/syncthing").asFile
	commandLine("git", "fetch", "--tags")
	isIgnoreExitValue = true // Don't crash if offline
}


// stupid helper because exec didn't work
object ShellRunner {
	fun runShellCommand(vararg args: String, workDir: File, env: Map<String, String>) {
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

		pb.inheritIO() // get output from running program
		println("Running command:\n\t${pb.command().joinToString("\n\t")}")
		val process = pb.start()
		val exitCode = process.waitFor()
		if (exitCode != 0) {
			error("Command failed with exit code $exitCode: ${args.joinToString(" ")}")
		}
	}
}

val buildNativeTasks = listOf("arm", "arm64", "x86", "x86_64").map { target ->

	tasks.register("buildNative_$target") {
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
		val ndkDir = rootProject.extra["ndk.dir"] as String


		// get_min_sdk(project_dir):
		val appBuildGradle = layout.projectDirectory.file("../app/build.gradle.kts").asFile
		val minSdk = appBuildGradle.readLines()
			.firstOrNull { it.contains("minSdk") }
			?.filter { it.isDigit() } ?: error("Could not find minSdk in build.gradle.kts")


		doLast {
			val ccPath =
				File("$ndkDir/toolchains/llvm/prebuilt/$ndkOs/bin/${ccTemplate.format(minSdk)}").absolutePath

			// Ensure build directories exist
			pkgDir.mkdirs()
			goCache.mkdirs()

			// Environment for Host Tools
			val hostEnv = mapOf(
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

			println("Building syncthing for ${target}...")
			val goExe = goBin.absolutePath

			ShellRunner.runShellCommand(goExe, "version", workDir = syncthingSrcDir, env = hostEnv)
			providers.exec {
				workingDir = syncthingSrcDir
				commandLine(goExe, "run", "build.go", "version")

				// Inject your environment
				environment(System.getenv())
				environment(hostEnv)

				// Explicitly add local Go to the PATH variable for this command execution
				val pathKey = if (System.getProperty("os.name").lowercase().contains("win")) "Path" else "PATH"
				environment(pathKey, "${goBin.parentFile.absolutePath}${File.pathSeparator}${System.getenv(pathKey)}")
			}.result.get()
			ShellRunner.runShellCommand(
				goExe,
				"run",
				"build.go",
				"version",
				workDir = syncthingSrcDir,
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

tasks.register("buildNative") {
	dependsOn(buildNativeTasks)
	doLast {
		println("All builds finished")
	}
}

/**
 * Use separate task instead of standard clean(), so these folders aren't deleted by `gradle clean`.
 */
tasks.register<Delete>("cleanNative") {
	delete("$projectDir/../app/src/main/jniLibs/")
	delete("gobuild")
}