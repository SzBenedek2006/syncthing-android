package dev.benedek.syncthingandroid.http

import android.content.Context
import com.android.volley.Request
import com.google.common.base.Optional
import java.net.URL

/**
 * Performs a GET request to the Syncthing API
 */
class GetRequest(
    context: Context, url: URL, path: String?, apiKey: String,
    params: MutableMap<String?, String?>?, listener: OnSuccessListener?
) : ApiRequest(context, url, path, apiKey) {
    init {
        val safeParams = Optional.fromNullable<MutableMap<String?, String?>>(params)
            .or(mutableMapOf())
        val uri = buildUri(safeParams)
        connect(Request.Method.GET, uri!!, null, listener, null)
    }

    companion object {
        const val URI_CONFIG: String = "/rest/config"
        const val URI_DEBUG: String = "/rest/debug"
        const val URI_VERSION: String = "/rest/system/version"
        const val URI_SYSTEM: String = "/rest/system/status"
        const val URI_CONNECTIONS: String = "/rest/system/connections"
        const val URI_STATUS: String = "/rest/db/status"
        const val URI_DEVICEID: String = "/rest/svc/deviceid"
        const val URI_REPORT: String = "/rest/svc/report"
        const val URI_EVENTS: String = "/rest/events"
    }
}
