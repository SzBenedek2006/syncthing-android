package dev.benedek.syncthingandroid.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.widget.ShareActionProvider
import androidx.core.view.MenuItemCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import dev.benedek.syncthingandroid.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * Shows the log information from Syncthing.
 */
class LogActivity : SyncthingActivity() {
    private var log: TextView? = null
    private var syncthingLog = true
    private var fetchLogJob: Job? = null
    private var scrollView: ScrollView? = null
    private var shareIntent: Intent? = null

    /**
     * Initialize Log.
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootView = layoutInflater.inflate(R.layout.activity_log, null)
        setContentView(rootView)
        setTitle(R.string.syncthing_log_title)

        // Targeting android 15 enables and 16 forces edge-to-edge,
        ViewCompat.setOnApplyWindowInsetsListener(
            rootView
        ) { v: View?, windowInsets: WindowInsetsCompat? ->
            val insets = windowInsets!!.getInsets(WindowInsetsCompat.Type.systemBars())
            val mlp = v!!.layoutParams as MarginLayoutParams
            mlp.leftMargin = insets.left
            mlp.bottomMargin = insets.bottom
            mlp.rightMargin = insets.right
            v.setLayoutParams(mlp)
            WindowInsetsCompat.CONSUMED
        }

        ViewCompat.setOnApplyWindowInsetsListener(
            rootView
        ) { v: View?, insets: WindowInsetsCompat? ->
            val bars = insets!!.getInsets(
                WindowInsetsCompat.Type.systemBars()
                        or WindowInsetsCompat.Type.displayCutout()
            )
            v!!.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }




        if (savedInstanceState != null) {
            syncthingLog = savedInstanceState.getBoolean("syncthingLog")
            invalidateOptionsMenu()
        }

        log = findViewById(R.id.log)
        scrollView = findViewById(R.id.scroller)

        updateLog()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("syncthingLog", syncthingLog)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.log_list, menu)

        val switchLog = menu.findItem(R.id.switch_logs)
        switchLog.setTitle(if (syncthingLog) R.string.view_android_log else R.string.view_syncthing_log)

        // Add the share button
        val shareItem = menu.findItem(R.id.menu_share)
        val actionProvider = MenuItemCompat.getActionProvider(shareItem) as ShareActionProvider?
        shareIntent = Intent()
        shareIntent!!.setAction(Intent.ACTION_SEND)
        shareIntent!!.setType("text/plain")
        shareIntent!!.putExtra(Intent.EXTRA_TEXT, log!!.getText())
        actionProvider!!.setShareIntent(shareIntent)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.switch_logs) {
            syncthingLog = !syncthingLog
            if (syncthingLog) {
                item.setTitle(R.string.view_android_log)
                setTitle(R.string.syncthing_log_title)
            } else {
                item.setTitle(R.string.view_syncthing_log)
                setTitle(R.string.android_log_title)
            }
            updateLog()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun updateLog() {

        fetchLogJob?.cancel()

        log!!.setText(R.string.retrieving_logs)
        fetchLogJob = lifecycleScope.launch {
            val logResult = getLogAsync(syncthingLog)
            log!!.text = logResult

            shareIntent?.putExtra(Intent.EXTRA_TEXT, logResult)

            scrollView!!.post {
                scrollView!!.scrollTo(0, log!!.bottom)
            }


        }
    }

    private suspend fun getLogAsync(syncthingLog: Boolean): String = withContext(Dispatchers.IO) {
        var process: Process? = null
        try {
            val pb = if (syncthingLog) {
                ProcessBuilder(
                    "/system/bin/logcat", "-t", "300", "-v", "time", "-s", "SyncthingNativeCode"
                )
            } else {
                ProcessBuilder(
                    "/system/bin/logcat", "-t", "300", "-v", "time", "*:i ps:s art:s"
                )
            }
            pb.redirectErrorStream(true)
            process = pb.start()

            val bufferedReader = BufferedReader(InputStreamReader(process.inputStream, "UTF-8"), 8192)
            val log = StringBuilder()
            var line: String?
            val sep = System.lineSeparator()

            while (bufferedReader.readLine().also { line = it } != null) {
                log.append(line)
                log.append(sep)
            }
            return@withContext log.toString()
        } catch (e: IOException) {
            Log.w(TAG, "Error reading Android log", e)
        } finally {
            process?.destroy()
        }
        return@withContext ""
    }

    companion object {
        private const val TAG = "LogActivity"
    }
}
