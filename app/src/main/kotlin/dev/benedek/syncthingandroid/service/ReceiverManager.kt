package dev.benedek.syncthingandroid.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.util.Log

object ReceiverManager {
    private const val TAG = "ReceiverManager"

    private val mReceivers: MutableList<BroadcastReceiver?> = ArrayList()

    @JvmStatic
    @Synchronized
    fun registerReceiver(
        context: Context,
        receiver: BroadcastReceiver?,
        intentFilter: IntentFilter?
    ) {
        mReceivers.add(receiver)
        // FIXME
        context.registerReceiver(receiver, intentFilter)
        Log.v(TAG, "Registered receiver: $receiver with filter: $intentFilter")
    }

    @Synchronized
    fun isReceiverRegistered(receiver: BroadcastReceiver?): Boolean {
        return mReceivers.contains(receiver)
    }

    @JvmStatic
    @Synchronized
    fun unregisterAllReceivers(context: Context?) {
        if (context == null) {
            Log.e(TAG, "unregisterReceiver: context is null")
            return
        }
        val iter = mReceivers.iterator()
        while (iter.hasNext()) {
            val receiver = iter.next()
            if (isReceiverRegistered(receiver)) {
                try {
                    context.unregisterReceiver(receiver)
                    Log.v(TAG, "Unregistered receiver: $receiver")
                } catch (_: IllegalArgumentException) {
                    // We have to catch the race condition a registration is still pending in android
                    // according to https://stackoverflow.com/a/3568906
                    Log.w(
                        TAG,
                        "unregisterReceiver($receiver) threw IllegalArgumentException"
                    )
                }
                iter.remove()
            }
        }
    }
}
