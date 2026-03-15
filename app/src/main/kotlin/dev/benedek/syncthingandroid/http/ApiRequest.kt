package dev.benedek.syncthingandroid.http

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import com.android.volley.DefaultRetryPolicy
import com.android.volley.NetworkResponse
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.HurlStack
import com.android.volley.toolbox.ImageRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.common.base.Function
import com.google.common.base.Optional
import com.google.common.collect.ImmutableMap
import dev.benedek.syncthingandroid.service.Constants.getHttpsCertFile
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import androidx.core.net.toUri
import java.nio.charset.Charset


abstract class ApiRequest internal constructor(
    private val context: Context,
    private val url: URL,
    private val path: String?,
    private val apiKey: String
) {
    fun interface OnSuccessListener {
        fun onSuccess(result: String?)
    }

    fun interface OnImageSuccessListener {
        fun onImageSuccess(result: Bitmap?)
    }

    fun interface OnErrorListener {
        fun onError(error: VolleyError?)
    }

    private val volleyQueue: RequestQueue
        get() {
            return Companion.volleyQueue ?:
            Volley.newRequestQueue(this.context.applicationContext, NetworkStack(this.sslSocketFactory))
                .also { Companion.volleyQueue = it }
        }

    fun buildUri(params: Map<String?, String?>): Uri? {
        val uriBuilder = url.toString().toUri().buildUpon().path(path)

        for (entry in params.entries) {
            uriBuilder.appendQueryParameter(entry.key, entry.value)
        }
        return uriBuilder.build()
    }

    /**
     * Opens the connection, then returns success status and response string.
     */
    fun connect(
        requestMethod: Int, uri: Uri, requestBody: String?,
        listener: OnSuccessListener?, errorListener: OnErrorListener?
    ) {
        Log.v(TAG, "Performing request to $uri")

        val request: StringRequest = object : StringRequest(
            requestMethod,
            uri.toString(),
            Response.Listener { reply -> listener?.onSuccess(reply)
            },
            Response.ErrorListener { error ->
                errorListener?.onError(error) ?:
                    Log.w(TAG, "Request to " + uri + " failed, " + error!!.message)
            }
        ) {
            override fun parseNetworkResponse(response: NetworkResponse): Response<String?> {
                try {
                    val charset = HttpHeaderParser.parseCharset(response.headers, "UTF-8")
                    val parsed = response.data.toString(Charset.forName(charset))

                    return Response.success(parsed, HttpHeaderParser.parseCacheHeaders(response))

                } catch (e: UnsupportedEncodingException) {
                    Log.e(this.toString(), e.toString())
                    return Response.success(
                        String(response.data, StandardCharsets.UTF_8),
                        HttpHeaderParser.parseCacheHeaders(response)
                    )
                }
            }

            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf(HEADER_API_KEY to apiKey)
            }

            override fun getBody(): ByteArray? {
                return requestBody?.toByteArray()

            }
        }

        // Some requests seem to be slow or fail, make sure this doesn't break the app
        // (e.g. if an event request fails, new event requests won't be triggered).
        request.retryPolicy = DefaultRetryPolicy(
            5000, 5,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        this.volleyQueue.add(request)
    }

    /**
     * Opens the connection, then returns success status and response bitmap.
     */
    fun makeImageRequest(
        uri: Uri, imageListener: OnImageSuccessListener?,
        errorListener: OnErrorListener?
    ) {
        val imageRequest: ImageRequest = object : ImageRequest(
            uri.toString(),
            Response.Listener { bitmap: Bitmap? ->
                imageListener?.onImageSuccess(bitmap)
            },
            0,
            0,
            ImageView.ScaleType.CENTER,
            Bitmap.Config.RGB_565,
            Response.ErrorListener { volleyError: VolleyError? ->
                errorListener?.onError(volleyError)
                Log.d(TAG, "onErrorResponse: $volleyError")
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf(HEADER_API_KEY to apiKey)
            }
        }

        this.volleyQueue.add(imageRequest)
    }

    /**
     * Extends [HurlStack], uses [.getSslSocketFactory] and disables hostname
     * verification.
     */
    private class NetworkStack(
        sslSocketFactory: SSLSocketFactory?
    ) : HurlStack(null, sslSocketFactory) {

        @Throws(IOException::class)
        override fun createConnection(url: URL?): HttpURLConnection? {
            if (url.toString().startsWith("https://")) {
                val connection = super.createConnection(url) as HttpsURLConnection
                connection.setHostnameVerifier { _: String?, _: SSLSession? -> true }
                return connection
            }
            return super.createConnection(url)
        }
    }

    private val sslSocketFactory: SSLSocketFactory?
        get() {
            try {
                val sslContext =
                    SSLContext.getInstance("TLS")
                val httpsCertPath =
                    getHttpsCertFile(context)
                sslContext.init(
                    null,
                    arrayOf<TrustManager>(SyncthingTrustManager(httpsCertPath)),
                    SecureRandom()
                )
                return sslContext.socketFactory
            } catch (e: NoSuchAlgorithmException) {
                Log.w(TAG, e)
                return null
            } catch (e: KeyManagementException) {
                Log.w(TAG, e)
                return null
            }
        }

    companion object {
        private const val TAG = "ApiRequest"

        /**
         * The name of the HTTP header used for the syncthing API key.
         */
        private const val HEADER_API_KEY = "X-API-Key"

        private var volleyQueue: RequestQueue? = null
    }
}
