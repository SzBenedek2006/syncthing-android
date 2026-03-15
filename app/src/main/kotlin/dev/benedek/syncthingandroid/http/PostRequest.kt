package dev.benedek.syncthingandroid.http

import android.content.Context
import com.android.volley.Request
import com.google.common.base.Optional
import java.net.URL

class PostRequest(
    context: Context, url: URL, path: String?, apiKey: String,
    params: MutableMap<String?, String?>?, listener: OnSuccessListener?
) : ApiRequest(context, url, path, apiKey) {
    init {
        val safeParams = Optional.fromNullable<MutableMap<String?, String?>>(params)
            .or(mutableMapOf())
        val uri = buildUri(safeParams)
        connect(Request.Method.POST, uri!!, null, listener, null)
    }

    companion object {
        const val URI_DB_OVERRIDE: String = "/rest/db/override"
    }
}
