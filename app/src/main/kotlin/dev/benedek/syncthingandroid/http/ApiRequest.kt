package dev.benedek.syncthingandroid.http

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import dev.benedek.syncthingandroid.service.Constants.getHttpsCertFile
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.net.URL
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager


abstract class ApiRequest internal constructor(
	private val context: Context,
	private val url: URL,
	private val path: String?,
	private val apiKey: String
) {
	private val client: HttpClient
		get() = Companion.client ?: HttpClient(Android) {
			expectSuccess = true // TODO: Check if necessary
			engine {
				connectTimeout = 5_000
				socketTimeout = 5_000
				sslManager = { connection ->
					connection.sslSocketFactory = sslSocketFactory // TODO: Check
					connection.hostnameVerifier = HostnameVerifier { _, _ -> true }
				}
			}
		}.also { Companion.client = it }

	fun buildUri(params: Map<String?, String?>): Uri? {
		val uriBuilder = url.toString().toUri().buildUpon().path(path)

		for (entry in params.entries) {
			uriBuilder.appendQueryParameter(entry.key, entry.value)
		}
		return uriBuilder.build()
	}

	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

	/**
	 * Opens the connection, then returns success status and response string.
	 */
	fun connect(
		requestMethod: HttpMethod,
		uri: Uri,
		requestBody: String?,
		listener: ((result: String?) -> Unit)?,
		errorListener: ((error: Throwable?) -> Unit)?
	) {
		Log.v(TAG, "Performing request to $uri")

		scope.launch {
			try {
				val response = client.request(uri.toString()) {
					method = requestMethod
					header(HEADER_API_KEY, apiKey)
					requestBody?.let { setBody(it) }
				}
				listener?.invoke(response.bodyAsText())
			} catch (e: Exception) {
				errorListener?.invoke(e)
				Log.w(TAG, "Request to $uri failed with exception ${e.javaClass.name}: ${e.message}")
			}
		}
	}

	/**
	 * Opens the connection, then returns success status and response bitmap.
	 */
	fun makeImageRequest(
		uri: Uri,
		onImageSuccessListener: ((result: Bitmap?) -> Unit)?,
		onErrorListener: ((error: Throwable?) -> Unit)?
	) {
		scope.launch {
			try {
				val bytes = client.get(uri.toString()) {
					header(HEADER_API_KEY, apiKey)
				}.body<ByteArray>()
				val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
				onImageSuccessListener?.invoke(bitmap)
			} catch (e: Exception) {
				onErrorListener?.invoke(e)
				Log.w(TAG, "Request to $uri failed with exception ${e.javaClass.name}: ${e.message}")
			}
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

		private var client: HttpClient? = null
	}
}
