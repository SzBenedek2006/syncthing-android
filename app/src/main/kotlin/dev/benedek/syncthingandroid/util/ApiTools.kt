package dev.benedek.syncthingandroid.util

import android.os.Build

/**
 * Run [block] if api level is at least [apiLevel] SDK, otherwise run [otherwise].
 *
 * @return What [block] returns if at least [apiLevel], else what [otherwise] returns.
 */
inline fun <T> atLeastSdk(apiLevel: Int, block: () -> T, otherwise: () -> T): T {
	return if (Build.VERSION.SDK_INT >= apiLevel) {
		block()
	} else {
		otherwise()
	}
}

/**
 * Run [block] if api level is at least [apiLevel] SDK.
 *
 * @return What [block] returns if at least [apiLevel], null otherwise.
 */
inline fun <T> atLeastSdk(apiLevel: Int, block: () -> T): T? {
	return if (Build.VERSION.SDK_INT >= apiLevel) {
		block()
	} else {
		null
	}
}

/**
 * Run [block] if api level is at least [apiLevel] FULL SDK, otherwise run [otherwise].
 *
 * @return What [block] returns if at least [apiLevel], else what [otherwise] returns.
 */
inline fun <T> atLeastSdkFull(apiLevel: Int, block: () -> T, otherwise: () -> T): T {
	return if (Build.VERSION.SDK_INT_FULL >= apiLevel) {
		block()
	} else {
		otherwise()
	}
}


/**
 * Run [block] if api level is at least [apiLevel] FULL SDK.
 *
 * @return What [block] returns if at least [apiLevel], null otherwise.
 */
inline fun <T> atLeastSdkFull(apiLevel: Int, block: () -> T): T? {
	return if (Build.VERSION.SDK_INT_FULL >= apiLevel) {
		block()
	} else {
		null
	}
}


// At most versions

/**
 * Run [block] if api level is at most [apiLevel] SDK, otherwise run [otherwise].
 *
 * @return What [block] returns if at most [apiLevel], else what [otherwise] returns.
 */
inline fun <T> atMostSdk(apiLevel: Int, block: () -> T, otherwise: () -> T): T {
	return if (Build.VERSION.SDK_INT <= apiLevel) {
		block()
	} else {
		otherwise()
	}
}

/**
 * Run [block] if api level is at most [apiLevel] SDK.
 *
 * @return What [block] returns if at most [apiLevel], null otherwise.
 */
inline fun <T> atMostSdk(apiLevel: Int, block: () -> T): T? {
	return if (Build.VERSION.SDK_INT <= apiLevel) {
		block()
	} else {
		null
	}
}

/**
 * Run [block] if api level is at most [apiLevel] FULL SDK, otherwise run [otherwise].
 *
 * @return What [block] returns if at most [apiLevel], else what [otherwise] returns.
 */
inline fun <T> atMostSdkFull(apiLevel: Int, block: () -> T, otherwise: () -> T): T {
	return if (Build.VERSION.SDK_INT_FULL <= apiLevel) {
		block()
	} else {
		otherwise()
	}
}

/**
 * Run [block] if api level is at most [apiLevel] FULL SDK.
 *
 * @return What [block] returns if at most [apiLevel], null otherwise.
 */
inline fun <T> atMostSdkFull(apiLevel: Int, block: () -> T): T? {
	return if (Build.VERSION.SDK_INT_FULL <= apiLevel) {
		block()
	} else {
		null
	}
}