@file:Suppress("LocalVariableName", "PrivatePropertyName")

package dev.benedek.syncthingandroid.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import dev.benedek.syncthingandroid.http.dto.DbStatus
import dev.benedek.syncthingandroid.http.dto.SystemConnections
import dev.benedek.syncthingandroid.model.Device
import dev.benedek.syncthingandroid.model.DeviceSort
import dev.benedek.syncthingandroid.model.Folder
import dev.benedek.syncthingandroid.model.FolderSort
import dev.benedek.syncthingandroid.model.SystemStatus
import dev.benedek.syncthingandroid.service.RestApi
import dev.benedek.syncthingandroid.service.SyncthingService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.Collections.reverseOrder
import kotlin.time.Duration.Companion.milliseconds

const val HISTORY_MAX_SIZE = 120

class MainViewModel : ViewModel() {

	private var serviceReference: WeakReference<SyncthingService>? = null
	val api: RestApi? get() = serviceReference?.get()?.api

	var fetchSystemDataJob: Job? = null

	var systemStatus by mutableStateOf<SystemStatus?>(null)
	val systemStatusHistory = mutableStateListOf<SystemStatus?>()

	var announceTotal: Int by mutableIntStateOf(0)
	var announceConnected: Int by mutableIntStateOf(0)

	val announceConnectedHistory = mutableStateListOf<Int>()

	// DIALOGS
	var showDeviceIdDialog by mutableStateOf(false)
	var showRestartDialog by mutableStateOf(false)
	var showExitDialog by mutableStateOf(false)

	var folders by mutableStateOf<List<Folder>?>(null)

	/**
	 * MutableStateFlow is better here because of the async nature of the api.
	 */
	var dbStatuses: MutableStateFlow<Map<String, DbStatus>> = MutableStateFlow(emptyMap())

	var devices by mutableStateOf<List<Device>?>(null)

	/**
	 * We get all the "connections" or "statuses" at once.
	 */
	var systemConnections by mutableStateOf(SystemConnections())
	val systemConnectionsHistory = mutableStateListOf<SystemConnections>()

	private val DEVICES_COMPARATOR =
		Comparator { lhs: Device?, rhs: Device? -> lhs!!.name.compareTo(rhs!!.name) }

	var apiCallDelay: Long = 100L
	val apiCallCount: Int = 3

	fun setService(service: SyncthingService) {
		serviceReference = WeakReference(service)
	}


	var foldersSortedBy by mutableStateOf(FolderSort.LABEL)
	var folderAscending by mutableStateOf(false)


	var devicesSortedBy by mutableStateOf(DeviceSort.NAME)
	var deviceAscending by mutableStateOf(false)


	private fun getFolderComparator(): Comparator<Folder> {
		val statuses = dbStatuses.value
		Log.d("stateChanged", statuses.values.toList().getOrNull(0)?.stateChanged.toString())
		Log.d("state", statuses.values.toList().getOrNull(0)?.state.toString())

		val comparator: Comparator<Folder> = when (foldersSortedBy) {
			FolderSort.LABEL -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.label ?: it.id ?: "" }
			FolderSort.DATE -> compareByDescending { statuses[it.id]?.stateChanged ?: "" }
			FolderSort.STATE -> compareBy { statuses[it.id]?.state }
			FolderSort.PAUSED -> compareByDescending { it.paused }
			FolderSort.LOCAL_SIZE -> compareByDescending { statuses[it.id]?.localBytes ?: 0L }
			FolderSort.GLOBAL_SIZE -> compareByDescending { statuses[it.id]?.globalBytes ?: 0L }
		}

		// TODO: comparator.reversed() when min API level 24 is set
		return if (!folderAscending) reverseOrder(comparator) else comparator
	}

	private fun getDeviceComparator(): Comparator<Device> {
		val connections = systemConnections.connections ?: emptyMap()

		val comparator: Comparator<Device> = when (devicesSortedBy) {
			DeviceSort.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
			// For strings in ISO-8601 format like "at" (timestamp), alphabetical descending works perfectly for dates
			DeviceSort.DATE -> compareByDescending { connections[it.deviceID]?.at ?: "" }
			DeviceSort.STATE -> compareBy { systemConnections.connections?.get(it.deviceID)?.state }
			// Map boolean to sort paused devices first
			DeviceSort.PAUSED -> compareByDescending { connections[it.deviceID]?.paused ?: false }
			DeviceSort.DOWNLOAD_SPEED -> compareByDescending { connections[it.deviceID]?.inBits ?: 0L }
			DeviceSort.UPLOAD_SPEED -> compareByDescending { connections[it.deviceID]?.outBits ?: 0L }
		}

		// TODO: comparator.reversed() when min API level 24 is set
		return if (!folderAscending) reverseOrder(comparator) else comparator
	}


	fun startFetchSystemData() {
		fetchSystemDataJob = viewModelScope.launch {
			val apiRefreshDelay: Long = (1000 - apiCallDelay * apiCallCount)
			while (isActive) {

				delay(apiRefreshDelay.milliseconds)

				api?.getSystemInfo { info -> // api call 1
					if (info != null) {
						systemStatus = info
						systemStatusHistory.add(info)
						announceTotal = systemStatus!!.discoveryMethods
						announceConnected = announceTotal - (systemStatus!!.discoveryErrors?.size ?: 0)
						announceConnectedHistory.add(announceConnected)
						while (announceConnectedHistory.size > HISTORY_MAX_SIZE) {
							announceConnectedHistory.remove(announceConnectedHistory.first())
						}
					}
				}
				delay(apiCallDelay.milliseconds)

				updateFolderStatuses()
				delay(apiCallDelay.milliseconds)

				val _devices = api?.getDevices(false).orEmpty().sortedWith(getDeviceComparator()) // api call 2
				devices = _devices

				delay(apiCallDelay.milliseconds)

				api?.getConnections { conn -> // api call 3
					if (conn != null) {
						systemConnections = conn
						systemConnectionsHistory.add(conn)
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

			Bitmap.createBitmap(pixels, width, height, Bitmap.Config.RGB_565)

		} catch (e: Exception) {
			e.printStackTrace()
			null
		}
	}


	private fun updateFolderStatuses() {
		val folders = api?.folders?.filterNotNull()?.sortedWith(getFolderComparator()) ?: return
		this.folders = folders

		for (folder in folders) {
			val folderId = folder.id ?: continue

			api?.getFolderStatus(folderId) { returnedId, status ->
				dbStatuses.update { currentMap ->
					currentMap + ((returnedId ?: "") to (status
						?: DbStatus())) // FIXME: Find a more robust way of doing this
				}
			}

		}
	}
}
