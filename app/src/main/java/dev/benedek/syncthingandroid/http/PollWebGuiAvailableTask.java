package dev.benedek.syncthingandroid.http;


import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.VolleyError;

import java.net.ConnectException;
import java.net.URL;
import java.util.Collections;

/**
 * Polls to load the web interface, until it is available.
 */
public class PollWebGuiAvailableTask extends ApiRequest {

    private static final String TAG = "PollWebGuiAvailableTask";
    /**
     * Interval in ms, at which connections to the web gui are performed on first start
     * to find out if it's online.
     */
    private static final long WEB_GUI_POLL_INTERVAL = 100;

    private final Handler handler = new Handler();

    private OnSuccessListener listener;

    private Integer logIncidence = 0;

    /**
     * Object that must be locked upon accessing mListener
     */
    private final Object listenerLock = new Object();

    public PollWebGuiAvailableTask(Context context, URL url, String apiKey,
                                   OnSuccessListener listener) {
        super(context, url, "", apiKey);
        Log.i(TAG, "Starting to poll for web gui availability");
        this.listener = listener;
        performRequest();
    }

    public void cancelRequestsAndCallback() {
        synchronized(listenerLock) {
            listener = null;
        }
    }

    private void performRequest() {
        Uri uri = buildUri(Collections.emptyMap());
        connect(Request.Method.GET, uri, null, this::onSuccess, this::onError);
    }

    private void onSuccess(String result) {
        synchronized(listenerLock) {
            if (listener != null) {
                listener.onSuccess(result);
            } else {
                Log.v(TAG, "Cancelled callback and outstanding requests");
            }
        }
    }

    private void onError(VolleyError error) {
        synchronized(listenerLock) {
            if (listener == null) {
                Log.v(TAG, "Cancelled callback and outstanding requests");
                return;
            }
        }

        handler.postDelayed(this::performRequest, WEB_GUI_POLL_INTERVAL);
        Throwable cause = error.getCause();
        if (cause == null || cause.getClass().equals(ConnectException.class)) {
            // Reduce lag caused by massively logging the same line while waiting.
            logIncidence++;
            if (logIncidence == 1 || logIncidence % 10 == 0) {
                Log.v(TAG, "Polling web gui ... (" + logIncidence + ")");
            }
        } else {
            Log.w(TAG, "Unexpected error while polling web gui", error);
        }
    }

}
