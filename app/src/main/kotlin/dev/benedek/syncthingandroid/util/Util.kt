package dev.benedek.syncthingandroid.util

import android.app.Activity
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.service.Constants
import eu.chainfire.libsuperuser.Shell
import java.io.BufferedWriter
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.io.OutputStreamWriter
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

object Util {
    private const val TAG = "SyncthingUtil"

    /**
     * Copies the given device ID to the clipboard (and shows a Toast telling about it).
     * 
     * @param id The device ID to copy.
     */
    fun copyDeviceId(context: Context, id: String?) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(context.getString(R.string.device_id), id)
        clipboard.setPrimaryClip(clip)
        if (Build.VERSION.SDK_INT < 33) {
            Toast.makeText(context, R.string.device_id_copied_to_clipboard, Toast.LENGTH_SHORT)
                .show()
        }
    }

    /**
     * Converts a number of bytes to a human readable file size (eg 3.5 GiB).
     * 
     * 
     * Based on [stackoverflow.com/a/5599842](http://stackoverflow.com/a/5599842)
     */
    fun readableFileSize(context: Context, bytes: Long): String {
        val units = context.resources.getStringArray(R.array.file_size_units)
        if (bytes <= 0) return "0 " + units[0]
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#")
            .format(bytes / 1024.0.pow(digitGroups.toDouble())) + " " + units[digitGroups]
    }

    /**
     * Converts a number of bytes to a human readable transfer rate in bytes per second
     * (eg 100 KiB/s).
     * 
     * 
     * Based on [stackoverflow.com/a/5599842](http://stackoverflow.com/a/5599842)
     */
    fun readableTransferRate(context: Context, bits: Long): String {
        val units = context.resources.getStringArray(R.array.transfer_rate_units)
        val bytes = bits / 8
        if (bytes <= 0) return "0 " + units[0]
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#")
            .format(bytes / 1024.0.pow(digitGroups.toDouble())) + " " + units[digitGroups]
    }

    /**
     * Normally an application's data directory is only accessible by the corresponding application.
     * Therefore, every file and directory is owned by an application's user and group. When running Syncthing as root,
     * it writes to the application's data directory. This leaves files and directories behind which are owned by root having 0600.
     * Moreover, those acitons performed as root changes a file's type in terms of SELinux.
     * A subsequent start of Syncthing will fail due to insufficient permissions.
     * Hence, this method fixes the owner, group and the files' type of the data directory.
     * 
     * @return true if the operation was successfully performed. False otherwise.
     */
    @JvmStatic
    fun fixAppDataPermissions(context: Context): Boolean {
        // We can safely assume that root magic is somehow available, because readConfig and saveChanges check for
        // read and write access before calling us.
        // Be paranoid :) and check if root is available.
        // Ignore the 'use_root' preference, because we might want to fix ther permission
        // just after the root option has been disabled.
        if (!Shell.SU.available()) {
            Log.e(TAG, "Root is not available. Cannot fix permissions.")
            return false
        }

        val packageName: String
        val appInfo: ApplicationInfo?
        try {
            packageName = context.packageName
            appInfo = context.packageManager.getApplicationInfo(packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            // This should not happen!
            // One should always be able to retrieve the application info for its own package.
            Log.w(TAG, "Error getting current package name", e)
            return false
        }
        Log.d(TAG, "Uid of '" + packageName + "' is " + appInfo.uid)

        // Get private app's "files" dir residing in "/data/data/[packageName]".
        val dir = context.filesDir.absolutePath
        var cmd = "chown -R " + appInfo.uid + ":" + appInfo.uid + " " + dir + "; "
        // Running Syncthing as root might change a file's or directories type in terms of SELinux.
        // Leaving them as they are, the Android service won't be able to access them.
        // At least for those files residing in an application's data folder.
        // Simply reverting the type to its default should do the trick.
        cmd += "restorecon -R $dir\n"
        Log.d(TAG, "Running: '$cmd")
        val exitCode = runShellCommand(cmd, true)
        if (exitCode == 0) {
            Log.i(TAG, "Fixed app data permissions on '$dir'.")
        } else {
            Log.w(
                TAG, "Failed to fix app data permissions on '" + dir + "'. Result: " +
                        exitCode
            )
        }
        return exitCode == 0
    }

    /**
     * Returns if the syncthing binary would be able to write a file into
     * the given folder given the configured access level.
     */
    fun nativeBinaryCanWriteToPath(context: Context, absoluteFolderPath: String?): Boolean {
        val TOUCH_FILE_NAME = ".stwritetest"
        var useRoot = false
        val prefUseRoot = PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(Constants.PREF_USE_ROOT, false)
        if (prefUseRoot && Shell.SU.available()) {
            useRoot = true
        }

        // Write permission test file.
        val touchFile = "$absoluteFolderPath/$TOUCH_FILE_NAME"
        val exitCode = runShellCommand("echo \"\" > \"$touchFile\"\n", useRoot)
        if (exitCode != 0) {
            val error =
                if (exitCode == 1) "Permission denied" else "Shell execution failed"

            Log.i(
                TAG, "Failed to write test file '" + touchFile +
                        "', " + error
            )
            return false
        }

        // Detected we have write permission.
        Log.i(TAG, "Successfully wrote test file '$touchFile'")

        // Remove test file.
        if (runShellCommand("rm \"$touchFile\"\n", useRoot) != 0) {
            // This is very unlikely to happen, so we have less error handling.
            Log.i(TAG, "Failed to remove test file")
        }
        return true
    }

    /**
     * Run command in a shell and return the exit code.
     */
    fun runShellCommand(cmd: String?, useRoot: Boolean): Int {
        // Assume "failure" exit code if an error is caught.
        var exitCode = 255
        var shellProc: Process? = null
        var shellOut: DataOutputStream? = null
        try {
            shellProc = Runtime.getRuntime().exec(if (useRoot) "su" else "sh")
            shellOut = DataOutputStream(shellProc.outputStream)
            val bufferedWriter = BufferedWriter(OutputStreamWriter(shellOut))
            Log.d(TAG, "runShellCommand: $cmd")
            bufferedWriter.write(cmd)
            bufferedWriter.flush()
            shellOut.close()
            shellOut = null
            exitCode = shellProc.waitFor()
        } catch (e: IOException) {
            Log.w(TAG, "runShellCommand: Exception", e)
        } catch (e: InterruptedException) {
            Log.w(TAG, "runShellCommand: Exception", e)
        } finally {
            try {
                shellOut?.close()
            } catch (e: IOException) {
                Log.w(TAG, "Failed to close shell stream", e)
            }
            shellProc?.destroy()
        }
        return exitCode
    }

    /**
     * Make sure that dialog is showing and activity is valid before dismissing dialog, to prevent
     * various crashes.
     */
    @JvmStatic
    fun dismissDialogSafe(dialog: Dialog?, activity: Activity) {
        if (dialog == null || !dialog.isShowing) return

        if (activity.isFinishing) return

        if (activity.isDestroyed) return

        dialog.dismiss()
    }

    /**
     * Format a path properly.
     * 
     * @param path String containing the path that needs formatting.
     * @return formatted file path as a string.
     */
    fun formatPath(path: String): String? {
        return File(path).toURI().normalize().getPath()
    }

    /**
     * @return a themed AlertDialog builder.
     */
    @JvmStatic
    fun getAlertDialogBuilder(context: Context): AlertDialog.Builder {
        return MaterialAlertDialogBuilder(context)
    }
}
