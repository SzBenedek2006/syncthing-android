package dev.benedek.syncthingandroid.activities

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.widget.Toolbar
import com.annimon.stream.Stream
import com.annimon.stream.function.Consumer
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.service.RestApi
import dev.benedek.syncthingandroid.service.SyncthingService
import dev.benedek.syncthingandroid.service.SyncthingServiceBinder
import java.util.LinkedList

/**
 * Connects to [SyncthingService] and provides access to it.
 */
abstract class SyncthingActivity : ThemedAppCompatActivity(), ServiceConnection {
    /**
     * Returns service object (or null if not bound).
     */
    var service: SyncthingService? = null
        private set

    private val mServiceConnectedListeners = LinkedList<OnServiceConnectedListener?>()

    /**
     * To be used for Fragments.
     */
    fun interface OnServiceConnectedListener {
        fun onServiceConnected()
    }

    /**
     * Look for a Toolbar in the layout and bind it as the activity's actionbar with reasonable
     * defaults.
     * 
     * The Toolbar must exist in the content view and have an id of R.id.toolbar. Trying to call
     * getSupportActionBar before this Activity's onPostCreate will cause a crash.
     */
    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        val toolbar = findViewById<Toolbar?>(R.id.toolbar)
        if (toolbar == null) return

        setSupportActionBar(toolbar)
        getSupportActionBar()!!.setDisplayHomeAsUpEnabled(true)
    }

    override fun onPause() {
        unbindService(this)
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        bindService(Intent(this, SyncthingService::class.java), this, BIND_AUTO_CREATE)
    }

    override fun onServiceConnected(componentName: ComponentName?, iBinder: IBinder?) {
        val syncthingServiceBinder = iBinder as SyncthingServiceBinder
        this.service = syncthingServiceBinder.service
        Stream.of<OnServiceConnectedListener?>(mServiceConnectedListeners)
            .forEach(Consumer { obj: OnServiceConnectedListener? -> obj!!.onServiceConnected() })
        mServiceConnectedListeners.clear()
    }

    override fun onServiceDisconnected(componentName: ComponentName?) {
        this.service = null
    }

    /**
     * Used for Fragments to use the Activity's service connection.
     */
    fun registerOnServiceConnectedListener(listener: OnServiceConnectedListener) {
        if (this.service != null) {
            listener.onServiceConnected()
        } else {
            mServiceConnectedListeners.addLast(listener)
        }
    }

    val api: RestApi?
        /**
         * Returns RestApi instance, or null if SyncthingService is not yet connected.
         */
        get() = if (this.service != null)
            this.service!!.api
        else
            null

    companion object {
        const val EXTRA_KEY_GENERATION_IN_PROGRESS: String =
            "dev.benedek.syncthing-android.SyncthingActivity.KEY_GENERATION_IN_PROGRESS"
    }
}
