package dev.benedek.syncthingandroid.viewmodel

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.model.Device
import dev.benedek.syncthingandroid.model.Folder
import dev.benedek.syncthingandroid.service.RestApi
import dev.benedek.syncthingandroid.service.SyncthingService
import java.lang.ref.WeakReference

class DeviceViewModel : ViewModel() {
	private var serviceReference: WeakReference<SyncthingService>? = null
	private val api: RestApi? get() = serviceReference?.get()?.api

	var device by mutableStateOf(Device(), policy = neverEqualPolicy())
		private set

	var isCreateMode by mutableStateOf(false)

	// TODO: Add list of folders to the UI with shared / unshared state
	data class FolderUiState(val folder: Folder, val isSelected: Boolean)

	var folderList = mutableStateListOf<FolderUiState>()
		private set

	var deviceNeedsToUpdate by mutableStateOf(false)
	var isValidDevice by mutableStateOf(false)
	var isInitialized by mutableStateOf(false)

	var addresses by mutableStateOf("")
	var displayName by mutableStateOf("")


	// DIALOGS
	var showDiscardDialog by mutableStateOf(false)
	var showCompressionDialog by mutableStateOf(false)
	var showDeleteDialog by mutableStateOf(false)

	fun setService(service: SyncthingService) {
		serviceReference = WeakReference(service)
	}

	fun setInitialState(
		context: Context,
		onFinish: () -> Unit,
		isCreate: Boolean,
		deviceId: String?,
		name: String?,
		addresses: MutableList<String?>,
		compression: String?,
		introducer: Boolean,
		paused: Boolean
	) {
		// Prevent resetting state on configuration changes
		if (isInitialized) return
		isInitialized = true

		isCreateMode = isCreate

		if (api != null) {
			if (isCreate) {
				initNewDevice(
					name,
					deviceId,
					addresses,
					compression,
					introducer,
					paused
				)
			} else {
				if (deviceId == null) {
					Toast.makeText(context, "deviceId == null", Toast.LENGTH_LONG).show()
					onFinish()
					return
				}
				loadExistingDevice(deviceId, onFinish, context)
			}

			this.addresses = addresses.joinToString()
			this.displayName = device.displayName
		}

		loadFolderList()
	}

	private fun loadExistingDevice(
		deviceId: String,
		onFinish: () -> Unit,
		context: Context
	) {
		val currentApi = api ?: return
		val devices = currentApi.getDevices(false) ?: emptyList<Device>()

		val found = devices.find { it.deviceID == deviceId }
		if (found == null) {
			onDone(context, onFinish)
			return
		}
		device = found

		// TODO
	}

	private fun initNewDevice(name: String?, deviceID: String?, addresses: List<String?>?, compression: String?, introducer: Boolean, paused: Boolean) {
		device = Device()
		device.name = name ?: ""
		device.deviceID = deviceID
		device.addresses = addresses
		device.compression = compression
		device.introducer = introducer
		device.paused = paused
	}

	fun loadFolderList() {
		// TODO
	}

	fun onDelete(onFinish: () -> Unit) {
		api?.removeDevice(device.deviceID)
		deviceNeedsToUpdate = false
		onFinish()
	}

	fun onCancel(onFinish: () -> Unit) {
		if (deviceNeedsToUpdate) {
			showDiscardDialog = true
		} else {
			onFinish()
		}
	}

	fun onDone(context: Context, onFinish: () -> Unit = {}) {
		if (deviceNeedsToUpdate) {
			onSave(context, onFinish)
		} else {
			onCancel(onFinish)
		}
	}

	fun onSave(context: Context, onFinish: () -> Unit) {
		Log.i(this.toString(), "onSave()")
		if (!deviceNeedsToUpdate) {
			onFinish()
			return
		}

		val currentApi = api

		// This shouldn't happen
		if (device.deviceID.isNullOrEmpty()) {
			Toast.makeText(context, R.string.device_id_required, Toast.LENGTH_LONG)
				.show()
			return
		}

		if (currentApi != null) {
			if (isCreateMode) {
				currentApi.addDevice(device) { error: String? ->
					if (!error.isNullOrEmpty()) {
						Toast.makeText(context, error, Toast.LENGTH_LONG).show()
					}
				}
			} else {
				currentApi.editDevice(device)
			}

			folderList.forEach { folderUiState ->
				val isAlreadyShared = folderUiState.folder.getDevice(device.deviceID) != null
				var needsUpdate = false

				if (folderUiState.isSelected && !isAlreadyShared) { // Add if added
					folderUiState.folder.addDevice(device.deviceID!!)
					needsUpdate = true
				} else if (!folderUiState.isSelected && isAlreadyShared) { // Remove if removed
					folderUiState.folder.removeDevice(device.deviceID)
					needsUpdate = true
				}

				if (needsUpdate) {
					// FIXME: Folders in uistate are empty
					currentApi.updateFolder(folderUiState.folder)
				}
			}

			deviceNeedsToUpdate = false
			onFinish()
		} else {
			Toast.makeText(
				context,
				R.string.syncthing_disabled,
				Toast.LENGTH_SHORT
			).show()
		}
	}

	fun onNameChange(value: String) {
		device.name = value
		device = device
		deviceNeedsToUpdate
	}

	fun onIdChange(value: String) {
		device.deviceID = value
		device = device
		deviceNeedsToUpdate = true
	}

	fun onAddressChange(value: String) {
		addresses = value
		val addressList = value.split(",")
			.map { it.trim() }
			.filter { it.isNotEmpty() }
		device.addresses = addressList
		device = device
		deviceNeedsToUpdate = true
	}

	fun onCompressionChange(value: String) {
		device.compression = value
		device = device
		deviceNeedsToUpdate = true
	}

	fun onIntroducerChange(checked: Boolean) {
		device.introducer = checked
		device = device
		deviceNeedsToUpdate = true
	}

	fun onPauseChange(checked: Boolean) {
		device.paused = checked
		device = device
		deviceNeedsToUpdate = true
	}

}
