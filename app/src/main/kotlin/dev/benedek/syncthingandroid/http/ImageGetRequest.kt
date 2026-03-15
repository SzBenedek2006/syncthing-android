package dev.benedek.syncthingandroid.http

import android.content.Context
import com.google.common.base.Optional
import java.net.URL

class ImageGetRequest(
    context: Context, url: URL, path: String?, apiKey: String,
    params: MutableMap<String?, String?>?,
    onSuccessListener: OnImageSuccessListener?, onErrorListener: OnErrorListener?
) : ApiRequest(context, url, path, apiKey) {
    init {
        val safeParams = Optional.fromNullable<MutableMap<String?, String?>>(params)
            .or(mutableMapOf())
        val uri = buildUri(safeParams)
        makeImageRequest(uri!!, onSuccessListener, onErrorListener)
    }

    companion object {
        const val QR_CODE_GENERATOR: String = "/qr/"
    }
}
