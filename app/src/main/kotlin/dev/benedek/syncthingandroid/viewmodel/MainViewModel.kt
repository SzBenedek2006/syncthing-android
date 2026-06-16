@file:Suppress("LocalVariableName", "PrivatePropertyName")

package dev.benedek.syncthingandroid.viewmodel

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import dev.benedek.syncthingandroid.model.Device
import dev.benedek.syncthingandroid.model.DeviceStatuses
import dev.benedek.syncthingandroid.model.Folder
import dev.benedek.syncthingandroid.model.FolderStatus
import dev.benedek.syncthingandroid.model.SystemInfo
import dev.benedek.syncthingandroid.service.RestApi
import dev.benedek.syncthingandroid.service.SyncthingService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

const val HISTORY_MAX_SIZE = 120

class MainViewModel : ViewModel() {
	val mainVisibilityObserver = MainVisibilityObserver()

	private var serviceReference: WeakReference<SyncthingService>? = null
	val api: RestApi? get() = serviceReference?.get()?.api
	var isApiReady by mutableStateOf(false)
		private set

	var fetchSystemDataJob: Job? = null

	var systemInfo by mutableStateOf<SystemInfo?>(null)
	val systemInfoHistory = mutableStateListOf<SystemInfo?>()

	var announceTotal: Int by mutableIntStateOf(0)
	var announceConnected: Int by mutableIntStateOf(0)

	val announceConnectedHistory = mutableStateListOf<Int>()

	// DIALOGS
	var showDeviceIdDialog by mutableStateOf(false)
	var showRestartDialog by mutableStateOf(false)
	var showExitDialog by mutableStateOf(false)

	var folders by mutableStateOf<List<Folder>>(emptyList())

	/**
	 * MutableStateFlow is better here because of the async nature of the api.
	 */
	var folderStatuses: MutableStateFlow<Map<String, FolderStatus>> = MutableStateFlow(emptyMap())

	var devices by mutableStateOf<List<Device>?>(emptyList())

	/**
	 * We get all the "connections" or "statuses" at once.
	 */
	var deviceStatuses by mutableStateOf(DeviceStatuses())
	val deviceStatusesHistory = mutableStateListOf<DeviceStatuses>()

	private val DEVICES_COMPARATOR =
		Comparator { lhs: Device?, rhs: Device? -> lhs!!.name.compareTo(rhs!!.name) }

	var apiCallDelay: Long = 100L
	val apiCallCount: Int = 3
	var apiRefreshDelay: Long = (1000 - apiCallDelay * apiCallCount)

	fun setService(service: SyncthingService) {
		serviceReference = WeakReference(service)
	}

	inner class MainVisibilityObserver : DefaultLifecycleObserver {


		override fun onStart(owner: LifecycleOwner) {
			super.onStart(owner)
			startFetchSystemData()
		}

		override fun onResume(owner: LifecycleOwner) {
			super.onResume(owner)

		}

		override fun onPause(owner: LifecycleOwner) {
			super.onPause(owner)

		}

		override fun onStop(owner: LifecycleOwner) {
			super.onStop(owner)
			stopFetchSystemData()
		}

	}


	init {
		viewModelScope.launch {
			while (true) {
				val _isApiReady = (api != null)

				if (isApiReady != _isApiReady) {
					if (!isApiReady) delay(apiCallDelay * apiCallCount)
					isApiReady = _isApiReady
				}

				delay(if (_isApiReady) 333L else 33L)
			}
		}
	}


	fun startFetchSystemData() {
		fetchSystemDataJob = viewModelScope.launch {
			while (isActive) {

				delay(apiRefreshDelay)

				api?.getSystemInfo { info -> // api call 1
					if (info != null) {
						systemInfo = info
						systemInfoHistory.add(info)
						announceTotal = systemInfo?.discoveryMethods ?: 0
						announceConnected = announceTotal - (systemInfo?.discoveryErrors?.size ?: 0)
						announceConnectedHistory.add(announceConnected)
						while (announceConnectedHistory.size > HISTORY_MAX_SIZE) {
							announceConnectedHistory.remove(announceConnectedHistory.first())
						}
					}
				}
				delay(apiCallDelay)

				updateFolderStatuses()
				delay(apiCallDelay)

				val _devices = api?.getDevices(false) // api call 2
				_devices?.sortWith(DEVICES_COMPARATOR)
				devices = _devices

				delay(apiCallDelay)

				api?.getConnections { conn -> // api call 3
					if (conn != null) {
						deviceStatuses = conn
						deviceStatusesHistory.add(conn)
						while (announceConnectedHistory.size > HISTORY_MAX_SIZE) {
							announceConnectedHistory.remove(announceConnectedHistory.first())
						}
					}
				}

			}
		}
	}

	fun stopFetchSystemData() {
		fetchSystemDataJob?.cancel()
		fetchSystemDataJob = null
	}

	fun generateQrBitmap(text: String?, size: Int = 328): Bitmap? {
		if (text.isNullOrEmpty()) return null

		return try {
			val bitMatrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
			val width = bitMatrix.width
			val height = bitMatrix.height

			val pixels = IntArray(width * height)

			for (y in 0 until height) {
				val offset = y * width
				for (x in 0 until width) {
					// BitMatrix is true for black, false for white
					pixels[offset + x] =
						if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
				}
			}

			// 4. Create Bitmap in one shot.
			// RGB_565 uses exactly half the memory of ARGB_8888!
			Bitmap.createBitmap(pixels, width, height, Bitmap.Config.RGB_565)

		} catch (e: Exception) {
			e.printStackTrace()
			null
		}
	}


	private fun updateFolderStatuses() {
		val folders = api?.folders?.filterNotNull() ?: return
		this.folders = folders

		for (folder in folders) {
			val folderId = folder.id ?: continue

			api?.getFolderStatus(folderId) { returnedId, status ->
				folderStatuses.update { currentMap ->
					currentMap + ((returnedId ?: "") to (status
						?: FolderStatus())) // FIXME: Find a more robust way of doing this
				}
			}

		}
	}
}