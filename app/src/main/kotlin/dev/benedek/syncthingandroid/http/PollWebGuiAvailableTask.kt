package dev.benedek.syncthingandroid.http

import android.content.Context
import android.os.Handler
import android.util.Log
import com.android.volley.Request
import com.android.volley.VolleyError
import java.net.ConnectException
import java.net.URL


/**
 * Polls to load the web interface, until it is available.
 */
class PollWebGuiAvailableTask(
    context: Context, url: URL, apiKey: String,
    listener: OnSuccessListener?
) : ApiRequest(context, url, "", apiKey) {
    private val handler = Handler()

    private var listener: OnSuccessListener?

    private var logIncidence = 0

    /**
     * Object that must be locked upon accessing mListener
     */
    private val listenerLock = Any()

    init {
        Log.i(TAG, "Starting to poll for web gui availability")
        this.listener = listener
        performRequest()
    }

    fun cancelRequestsAndCallback() {
        synchronized(listenerLock) {
            listener = null
        }
    }

    private fun performRequest() {
        val uri = buildUri(mutableMapOf())
        connect(
            Request.Method.GET,
            uri!!,
            null,
            { result: String? -> this.onSuccess(result) },
            { error: VolleyError? -> this.onError(error!!) })
    }

    private fun onSuccess(result: String?) {
        synchronized(listenerLock) {
            if (listener != null) {
                listener!!.onSuccess(result)
            } else {
                Log.v(TAG, "Cancelled callback and outstanding requests")
            }
        }
    }

    private fun onError(error: VolleyError) {
        synchronized(listenerLock) {
            if (listener == null) {
                Log.v(TAG, "Cancelled callback and outstanding requests")
                return
            }
        }

        handler.postDelayed({ this.performRequest() }, WEB_GUI_POLL_INTERVAL)
        val cause = error.cause
        if (cause == null || cause.javaClass == ConnectException::class.java) {
            // Reduce lag caused by massively logging the same line while waiting.
            logIncidence++
            if (logIncidence == 1 || logIncidence % 10 == 0) {
                Log.v(TAG, "Polling web gui ... ($logIncidence)")
            }
        } else {
            Log.w(TAG, "Unexpected error while polling web gui", error)
        }
    }

    companion object {
        private const val TAG = "PollWebGuiAvailableTask"

        /**
         * Interval in ms, at which connections to the web gui are performed on first start
         * to find out if it's online.
         */
        private const val WEB_GUI_POLL_INTERVAL: Long = 100
    }
}
