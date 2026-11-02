package dev.benedek.syncthingandroid.http

import android.content.Context
import com.google.common.base.Optional
import io.ktor.http.HttpMethod.Companion.Post
import java.net.URL

class PostRequest(
	context: Context,
	url: URL,
	path: String?,
	apiKey: String,
	params: MutableMap<String?, String?>?,
	onSuccessListener: ((result: String?) -> Unit)?
) : ApiRequest(context, url, path, apiKey) {
	init {
		val safeParams = Optional.fromNullable(params)
			.or(mutableMapOf())
		val uri = buildUri(safeParams)
		connect(Post, uri!!, null, onSuccessListener, null)
	}

	companion object {
		const val URI_DB_OVERRIDE: String = "/rest/db/override"
	}
}
