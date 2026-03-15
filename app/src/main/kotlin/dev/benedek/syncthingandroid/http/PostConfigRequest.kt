package dev.benedek.syncthingandroid.http

import android.content.Context
import com.android.volley.Request
import java.net.URL

class PostConfigRequest(
    context: Context, url: URL, apiKey: String, config: String?,
    listener: OnSuccessListener?
) : ApiRequest(context, url, URI_CONFIG, apiKey) {
    init {
        val uri = buildUri(mutableMapOf())
        connect(Request.Method.POST, uri!!, config, listener, null)
    }

    companion object {
        private const val URI_CONFIG = "/rest/system/config"
    }
}
