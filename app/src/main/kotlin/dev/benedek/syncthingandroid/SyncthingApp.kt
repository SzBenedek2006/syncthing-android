package dev.benedek.syncthingandroid

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.system.Os
import android.util.Log
import com.google.android.material.color.DynamicColors
import dev.benedek.syncthingandroid.util.Languages
import dev.benedek.syncthingandroid.util.ThemeControls
import java.io.File
import java.io.FileOutputStream

class SyncthingApp : Application() {
    override fun onCreate() {
        if (ThemeControls.isMonetEnabled) DynamicColors.applyToActivitiesIfAvailable(this)

        super.onCreate()
        ThemeControls.init(this)

        setupLegacySsl(this)


        Languages(this).setLanguage(this)

        // The main point here is to use a VM policy without
        // `detectFileUriExposure`, as that leads to exceptions when e.g.
        // opening the ignores file. And it's enabled by default.
        // We might want to disable `detectAll` and `penaltyLog` on release (non-RC) builds too.
        val policy = VmPolicy.Builder()
            .detectAll()
            .penaltyLog()
            .build()
        StrictMode.setVmPolicy(policy)
    }

    private fun setupLegacySsl(context: Context) {
        // We only need this hack for Android 7.0 (API 24) and below.
        // Android 7.1 (API 25) supports these certs natively.
        if (Build.VERSION.SDK_INT <= 24) {
            try {
                val certName = "isrgrootx1.pem"
                // Result: /data/user/0/dev.benedek.syncthingandroid.debug/files/isrgrootx1.pem
                val certFile = File(context.getFilesDir(), certName)

                // Copy from assets to internal storage if not already there
                if (!certFile.exists()) {
                    Log.d(
                        this.toString(),
                        "File(context.getFilesDir(), certName) didn't find the file"
                    )
                    context.getAssets().open(certName).use { `in` ->
                        FileOutputStream(certFile).use { out ->
                            val buffer = ByteArray(1024)
                            var read: Int
                            while ((`in`.read(buffer).also { read = it }) != -1) {
                                out.write(buffer, 0, read)
                            }
                        }
                    }
                }

                Os.setenv("SSL_CERT_FILE", certFile.getAbsolutePath(), true)
                Log.i(
                    "SyncthingApp",
                    "Legacy SSL Hack applied using: " + certFile.getAbsolutePath()
                )
            } catch (e: Exception) {
                Log.e("SyncthingApp", "Failed to apply Legacy SSL Hack", e)
            }
        }
    }
}
