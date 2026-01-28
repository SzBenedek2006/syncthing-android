package dev.benedek.syncthingandroid.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Environment
import android.os.PowerManager
import android.os.SystemClock
import android.text.TextUtils
import android.util.Log
import com.google.common.io.FileWriteMode
import com.google.common.io.Files
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.SyncthingApp
import dev.benedek.syncthingandroid.util.Util
import eu.chainfire.libsuperuser.Shell
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.DataOutputStream
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.LineNumberReader
import java.nio.charset.StandardCharsets
import java.security.InvalidParameterException
import java.util.HashSet
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

/**
 * Runs the syncthing binary from command line, and prints its output to logcat.
 *
 * @see [Command Line Docs](http://docs.syncthing.net/users/syncthing.html)
 */
class SyncthingRunnable(context: Context, command: Command) : Runnable {
    private val mContext: Context
    private val mSyncthingBinary: File
    private val mCommand: Array<String?>
    private val mLogFile: File

    @JvmField
    @Inject
    var mPreferences: SharedPreferences? = null
    private val mUseRoot: Boolean

    @JvmField
    @Inject
    var mNotificationHandler: NotificationHandler? = null

    enum class Command {
        deviceid,  // Output the device ID to the command line.
        generate,  // Generate keys, a config file and immediately exit.
        main,  // Run the main Syncthing application.
        resetdatabase,  // Reset Syncthing's database
        resetdeltas,  // Reset Syncthing's delta indexes
    }

    /**
     * Constructs instance.
     *
     * @param command Which type of Syncthing command to execute.
     */
    init {
        (context.applicationContext as SyncthingApp).component().inject(this)
        mContext = context
        mSyncthingBinary = Constants.getSyncthingBinary(mContext)
        mLogFile = Constants.getLogFile(mContext)

        // Get preferences relevant to starting syncthing core.
        mUseRoot = mPreferences!!.getBoolean(Constants.PREF_USE_ROOT, false) && Shell.SU.available()
        @Suppress("REDUNDANT_ELSE_IN_WHEN")
        when (command) {
            Command.deviceid ->                 // CHANGED: "device-id" is now a subcommand
                mCommand = arrayOf<String?>(
                    mSyncthingBinary.path,
                    "device-id",
                    "--home",
                    mContext.filesDir.toString()
                )

            Command.generate ->                 // CHANGED: "generate" is now a subcommand, and "-logflags" is removed
                mCommand = arrayOf<String?>(
                    mSyncthingBinary.path,
                    "generate",
                    "--home",
                    mContext.filesDir.toString()
                )

            Command.main ->                 // CHANGED: "-logflags" is removed. The default logging is fine.
                mCommand = arrayOf<String?>(
                    mSyncthingBinary.path,
                    "--home",
                    mContext.filesDir.toString(),
                    "--no-browser"
                )

            Command.resetdatabase ->                 // CHANGED: "reset-database" is now a CLI subcommand
                mCommand = arrayOf<String?>(
                    mSyncthingBinary.path,
                    "cli",
                    "database",
                    "reset",
                    "--home",
                    mContext.filesDir.toString()
                )

            Command.resetdeltas ->                 // CHANGED: "reset-deltas" is now a CLI subcommand
                mCommand = arrayOf<String?>(
                    mSyncthingBinary.path,
                    "cli",
                    "deltas",
                    "reset",
                    "--home",
                    mContext.filesDir.toString()
                )

            else -> throw InvalidParameterException("Unknown command option")
        }
    }

    override fun run() {
        run(false)
    }

    @SuppressLint("WakelockTimeout", "Wakelock")
    fun run(returnStdOut: Boolean): String {
        trimLogFile()
        val ret: Int
        val capturedStdOut = StringBuilder()
        // Make sure Syncthing is executable
        try {
            val pb = ProcessBuilder("chmod", "500", mSyncthingBinary.path)
            val p = pb.start()
            p.waitFor()
        } catch (e: IOException) {
            Log.w(TAG, "Failed to chmod Syncthing", e)
        } catch (e: InterruptedException) {
            Log.w(TAG, "Failed to chmod Syncthing", e)
        }
        // Loop Syncthing
        var process: Process? = null
        // Potential fix for #498, keep the CPU running while native binary is running
        val pm = mContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = if (useWakeLock())
            pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                mContext.getString(R.string.app_name) + ":" + TAG
            )
        else
            null
        try {
            wakeLock?.acquire()
            increaseInotifyWatches()

            val targetEnv = buildEnvironment()
            process = setupAndLaunch(targetEnv)

            mSyncthing.set(process)

            var lInfo: Thread? = null
            var lWarn: Thread? = null
            if (returnStdOut) {
                try {
                    BufferedReader(
                        InputStreamReader(
                            process.inputStream,
                            StandardCharsets.UTF_8
                        )
                    ).use { br ->
                        var line: String?
                        while ((br.readLine().also { line = it }) != null) {
                            Log.println(Log.INFO, TAG_NATIVE, line!!)
                            capturedStdOut.append(line).append("\n")
                        }
                    }
                } catch (e: IOException) {
                    Log.w(TAG, "Failed to read Syncthing's command line output", e)
                }
            } else {
                lInfo = log(process.inputStream, Log.INFO, true)
                lWarn = log(process.errorStream, Log.WARN, true)
            }

            niceSyncthing()

            ret = process.waitFor()
            Log.i(TAG, "Syncthing exited with code $ret")
            mSyncthing.set(null)
            lInfo?.join()
            lWarn?.join()

            when (ret) {
                0, 137 -> {}
                1 -> {
                    Log.w(
                        TAG,
                        "Another Syncthing instance is already running, requesting restart via SyncthingService intent"
                    )
                    // Restart was requested via Rest API call.
                    Log.i(TAG, "Restarting syncthing")
                    mContext.startService(
                        Intent(mContext, SyncthingService::class.java)
                            .setAction(SyncthingService.ACTION_RESTART)
                    )
                }

                3 -> {
                    Log.i(TAG, "Restarting syncthing")
                    mContext.startService(
                        Intent(mContext, SyncthingService::class.java)
                            .setAction(SyncthingService.ACTION_RESTART)
                    )
                }

                else -> {
                    Log.w(TAG, "Syncthing has crashed (exit code $ret)")
                    mNotificationHandler!!.showCrashedNotification(
                        R.string.notification_crash_title,
                        false
                    )
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to execute syncthing binary or read output", e)
        } catch (e: InterruptedException) {
            Log.e(TAG, "Failed to execute syncthing binary or read output", e)
        } finally {
            wakeLock?.release()
            process?.destroy()
        }
        return capturedStdOut.toString()
    }

    private fun putCustomEnvironmentVariables(
        environment: MutableMap<String?, String?>,
        sp: SharedPreferences
    ) {
        val customEnvironment = sp.getString("environment_variables", null)
        if (TextUtils.isEmpty(customEnvironment)) return

        for (e in customEnvironment!!.split(" ".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()) {
            val e2 = e.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            environment[e2[0]] = e2[1]
        }
    }

    /**
     * Returns true if the experimental setting for using wake locks has been enabled in settings.
     */
    private fun useWakeLock(): Boolean {
        return mPreferences!!.getBoolean(Constants.PREF_USE_WAKE_LOCK, false)
    }

    private val syncthingPIDs: MutableList<String?>
        /**
         * Look for running libsyncthing.so processes and return an array
         * containing the PIDs of found instances.
         */
        get() {
            val syncthingPIDs: MutableList<String?> =
                ArrayList()
            var ps: Process? = null
            var psOut: DataOutputStream? = null
            var br: BufferedReader? = null
            try {
                ps = Runtime.getRuntime().exec(if (mUseRoot) "su" else "sh")
                psOut = DataOutputStream(ps.outputStream)
                psOut.writeBytes("ps\n")
                psOut.writeBytes("exit\n")
                psOut.flush()
                ps.waitFor()
                br = BufferedReader(
                    InputStreamReader(
                        ps.inputStream,
                        StandardCharsets.UTF_8
                    )
                )
                var line: String?
                while ((br.readLine().also { line = it }) != null) {
                    if (line!!.contains(Constants.FILENAME_SYNCTHING_BINARY)) {
                        val syncthingPID: String =
                            line.trim { it <= ' ' }.split("\\s+".toRegex())
                                .dropLastWhile { it.isEmpty() }.toTypedArray()[1]
                        Log.v(
                            TAG,
                            "getSyncthingPIDs: Found process PID [$syncthingPID]"
                        )
                        syncthingPIDs.add(syncthingPID)
                    }
                }
            } catch (e: IOException) {
                Log.w(
                    TAG,
                    "Failed to list Syncthing processes",
                    e
                )
            } catch (e: InterruptedException) {
                Log.w(
                    TAG,
                    "Failed to list Syncthing processes",
                    e
                )
            } finally {
                try {
                    br?.close()
                    psOut?.close()
                } catch (e: IOException) {
                    Log.w(
                        TAG,
                        "Failed to close psOut stream",
                        e
                    )
                }
                ps?.destroy()
            }
            return syncthingPIDs
        }

    /**
     * Root-only: Temporarily increase "fs.inotify.max_user_watches"
     * as Android has a default limit of 8192 watches.
     * Manually run "sysctl fs.inotify" in a root shell terminal to check current limit.
     */
    private fun increaseInotifyWatches() {
        if (!mUseRoot || !Shell.SU.available()) {
            Log.i(
                TAG,
                "increaseInotifyWatches: Root is not available. Cannot increase inotify limit."
            )
            return
        }
        val exitCode =
            Util.runShellCommand("sysctl -n -w fs.inotify.max_user_watches=131072\n", true)
        Log.i(TAG, "increaseInotifyWatches: sysctl returned $exitCode")
    }

    /**
     * Look for a running libsyncthing.so process and nice its IO.
     */
    private fun niceSyncthing() {
        if (!mUseRoot || !Shell.SU.available()) {
            Log.i(TAG_NICE, "Root is not available. Cannot nice syncthing.")
            return
        }

        val syncthingPIDs = this.syncthingPIDs
        if (syncthingPIDs.isEmpty()) {
            Log.i(TAG_NICE, "Found no running instances of " + Constants.FILENAME_SYNCTHING_BINARY)
            return
        }

        // Ionice all running syncthing processes.
        for (syncthingPID in syncthingPIDs) {
            // Set best-effort, low priority using ionice.
            val exitCode =
                Util.runShellCommand("/system/bin/ionice $syncthingPID be 7\n", true)
            Log.i(
                TAG_NICE, "ionice returned " + exitCode +
                        " on " + Constants.FILENAME_SYNCTHING_BINARY
            )
        }
    }

    interface OnSyncthingKilled {
        fun onKilled()
    }

    /**
     * Look for running libsyncthing.so processes and kill them.
     * Try a SIGINT first, then try again with SIGKILL.
     */
    fun killSyncthing() {
        for (i in 0..1) {
            val syncthingPIDs = this.syncthingPIDs
            if (syncthingPIDs.isEmpty()) {
                Log.d(
                    TAG,
                    "killSyncthing: Found no more running instances of " + Constants.FILENAME_SYNCTHING_BINARY
                )
                break
            }

            var exitCode: Int
            for (syncthingPID in syncthingPIDs) {
                if (i > 0) {
                    // Force termination of the process by sending SIGKILL.
                    SystemClock.sleep(3000)
                    exitCode =
                        Util.runShellCommand("kill -SIGKILL $syncthingPID\n", mUseRoot)
                } else {
                    exitCode = Util.runShellCommand("kill -SIGINT $syncthingPID\n", mUseRoot)
                    SystemClock.sleep(1000)
                }
                if (exitCode == 0) {
                    Log.d(TAG, "Killed Syncthing process $syncthingPID")
                } else {
                    Log.w(
                        TAG, "Failed to kill Syncthing process " + syncthingPID +
                                " exit code " + exitCode
                    )
                }
            }
        }
    }

    /**
     * Logs the outputs of a stream to logcat and mNativeLog.
     *
     * @param is The stream to log.
     * @param priority The priority level.
     * @param saveLog True if the log should be stored to [.mLogFile].
     */
    @Suppress("SameParameterValue")
    private fun log(`is`: InputStream?, priority: Int, saveLog: Boolean): Thread {
        val t = Thread {
            var br: BufferedReader? = null
            try {
                br = BufferedReader(InputStreamReader(`is`, StandardCharsets.UTF_8))
                var line: String?
                while ((br.readLine().also { line = it }) != null) {
                    Log.println(priority, TAG_NATIVE, line!!)

                    if (saveLog) {
                        Files.asCharSink(mLogFile, StandardCharsets.UTF_8, FileWriteMode.APPEND)
                            .write(line)
                    }
                }
            } catch (e: IOException) {
                Log.w(TAG, "Failed to read Syncthing's command line output", e)
            }
            try {
                br!!.close()
            } catch (e: IOException) {
                Log.w(TAG, "log: Failed to close bufferedReader", e)
            }
        }
        t.start()
        return t
    }

    /**
     * Only keep last [.LOG_FILE_MAX_LINES] lines in log file, to avoid bloat.
     */
    private fun trimLogFile() {
        if (!mLogFile.exists()) return

        try {
            val lnr = LineNumberReader(FileReader(mLogFile))
            lnr.skip(Long.MAX_VALUE)

            val lineCount = lnr.lineNumber
            lnr.close()

            val tempFile = File(mContext.getExternalFilesDir(null), "syncthing.log.tmp")

            val reader = BufferedReader(FileReader(mLogFile))
            val writer = BufferedWriter(FileWriter(tempFile))

            var currentLine: String?
            val startFrom: Int = lineCount - LOG_FILE_MAX_LINES
            var i = 0
            while ((reader.readLine().also { currentLine = it }) != null) {
                if (i > startFrom) {
                    writer.write(currentLine + "\n")
                }
                i++
            }
            writer.close()
            reader.close()
            if (!tempFile.renameTo(mLogFile)) {
                Log.w(TAG, "Failed to rename $tempFile to $mLogFile.")
            }
        } catch (e: IOException) {
            Log.w(TAG, "Failed to trim log file", e)
        }
    }

    private fun buildEnvironment(): HashMap<String?, String?> {
        val targetEnv = HashMap<String?, String?>()
        // Set home directory to data folder for web GUI folder picker.
        targetEnv["HOME"] = Environment.getExternalStorageDirectory().absolutePath
        targetEnv["STTRACE"] = TextUtils.join(
            " ",
            mPreferences!!.getStringSet(
                Constants.PREF_DEBUG_FACILITIES_ENABLED,
                HashSet()
            )!!
        )
        val externalFilesDir: File = checkNotNull(mContext.getExternalFilesDir(null))
        Log.d("externalFilesDir", externalFilesDir.toString())
        targetEnv["STGUIASSETS"] = externalFilesDir.absolutePath + "/gui"
        targetEnv["STMONITORED"] = "1"
        targetEnv["STNOUPGRADE"] = "1"
        // Disable hash benchmark for faster startup.
        // https://github.com/syncthing/syncthing/issues/4348
        targetEnv["STHASHING"] = "minio"
        if (mPreferences!!.getBoolean(Constants.PREF_USE_TOR, false)) {
            targetEnv["all_proxy"] = "socks5://localhost:9050"
            targetEnv["ALL_PROXY_NO_FALLBACK"] = "1"
        } else {
            val socksProxyAddress: String = mPreferences!!.getString(
                Constants.PREF_SOCKS_PROXY_ADDRESS,
                ""
            )!!
            if (!socksProxyAddress.isEmpty()) {
                targetEnv["all_proxy"] = socksProxyAddress
            }

            val httpProxyAddress: String = mPreferences!!.getString(
                Constants.PREF_HTTP_PROXY_ADDRESS,
                ""
            )!!
            if (!httpProxyAddress.isEmpty()) {
                targetEnv["http_proxy"] = httpProxyAddress
                targetEnv["https_proxy"] = httpProxyAddress
            }
        }
        if (mPreferences!!.getBoolean("use_legacy_hashing", false)) targetEnv["STHASHING"] = "standard"
        putCustomEnvironmentVariables(targetEnv, mPreferences!!)
        return targetEnv
    }

    @Throws(IOException::class)
    private fun setupAndLaunch(env: HashMap<String?, String?>): Process {
        if (mUseRoot) {
            val pb = ProcessBuilder("su")
            val process = pb.start()
            // The su binary prohibits the inheritance of environment variables.
            // Even with --preserve-environment the environment gets messed up.
            // We therefore start a root shell, and set all the environment variables manually.
            val suOut = DataOutputStream(process.outputStream)
            for (entry in env.entries) {
                suOut.writeBytes(String.format("export %s=\"%s\"\n", entry.key, entry.value))
            }
            suOut.flush()
            // Exec will replace the su process image by Syncthing as execlp in C does.
            // Without using exec, the process will drop to the root shell as soon as Syncthing terminates like a normal shell does.
            // If we did not use exec, we would wait infinitely for the process to terminate (ret = process.waitFor(); in run()).
            // With exec the whole process terminates when Syncthing exits.
            suOut.writeBytes("exec " + TextUtils.join(" ", mCommand) + "\n")
            // suOut.flush has to be called to fix issue - #1005 Endless loader after enabling "Superuser mode"
            suOut.flush()
            return process
        } else {
            val pb = ProcessBuilder(*mCommand)
            pb.environment().putAll(env)
            return pb.start()
        }
    }

    companion object {
        private const val TAG = "SyncthingRunnable"
        private const val TAG_NATIVE = "SyncthingNativeCode"
        private const val TAG_NICE = "SyncthingRunnableIoNice"
        private const val LOG_FILE_MAX_LINES = 10

        private val mSyncthing = AtomicReference<Process?>()
    }
}