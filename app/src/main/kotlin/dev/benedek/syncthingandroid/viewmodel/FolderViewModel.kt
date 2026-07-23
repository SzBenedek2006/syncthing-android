package dev.benedek.syncthingandroid.viewmodel

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.model.Device
import dev.benedek.syncthingandroid.model.Folder
import dev.benedek.syncthingandroid.service.Constants
import dev.benedek.syncthingandroid.service.RestApi
import dev.benedek.syncthingandroid.service.SyncthingService
import dev.benedek.syncthingandroid.util.FileUtils
import dev.benedek.syncthingandroid.util.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import kotlin.random.Random

@OptIn(SavedStateHandleSaveableApi::class)
class FolderViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
	private var serviceReference: WeakReference<SyncthingService>? = null
	private val api: RestApi? get() = serviceReference?.get()?.api

	var folder by savedStateHandle.saveable {
		mutableStateOf(Folder())
	}
		private set
	var folderUri: Uri by mutableStateOf(Uri.EMPTY)

	var isCreateMode by savedStateHandle.saveable { mutableStateOf(false) }
	var canWriteToPath by mutableStateOf(false)
		private set

	data class DeviceUiState(val device: Device, val isSelected: Boolean)

	var deviceList = mutableStateListOf<DeviceUiState>()
		private set
	private var folderNeedsToUpdate by savedStateHandle.saveable { mutableStateOf(false) }
	private var versioning: Folder.Versioning? = null
	private var isInitialized: Boolean by savedStateHandle.saveable { mutableStateOf(false) }
	var isPathWritable by mutableStateOf(false)

	// FOLDER TYPE STUFF
	// TODO: Move these to Constants or Util
	data class FolderType(
		val value: String,
		val titleRes: Int,
		val descriptionRes: Int
	)

	val folderType = listOf(
		FolderType(
			Constants.FOLDER_TYPE_SEND_RECEIVE,
			R.string.folder_type_sendreceive,
			R.string.folder_type_sendreceive_description
		),
		FolderType(
			Constants.FOLDER_TYPE_SEND_ONLY,
			R.string.folder_type_sendonly,
			R.string.folder_type_sendonly_description
		),
		FolderType(
			Constants.FOLDER_TYPE_RECEIVE_ONLY,
			R.string.folder_type_receiveonly,
			R.string.folder_type_receiveonly_description
		)
	)

	fun getCurrentFolderType(): FolderType {
		return folderType.find { it.value == folder.type }!!
	}

	data class FolderPullOrder(
		val value: String,
		val titleRes: Int,
		val descriptionRes: Int
	)

	val folderPullOrders = listOf(
		FolderPullOrder(
			Constants.FOLDER_PULL_ORDER_RANDOM,
			R.string.pull_order_type_random,
			R.string.pull_order_type_random_description
		),
		FolderPullOrder(
			Constants.FOLDER_PULL_ORDER_ALPHABETIC,
			R.string.pull_order_type_alphabetic,
			R.string.pull_order_type_alphabetic_description
		),
		FolderPullOrder(
			Constants.FOLDER_PULL_ORDER_NEWEST_FIRST,
			R.string.pull_order_type_newestFirst,
			R.string.pull_order_type_newestFirst_description
		),
		FolderPullOrder(
			Constants.FOLDER_PULL_ORDER_OLDEST_FIRST,
			R.string.pull_order_type_oldestFirst,
			R.string.pull_order_type_oldestFirst_description
		),
		FolderPullOrder(
			Constants.FOLDER_PULL_ORDER_LARGEST_FIRST,
			R.string.pull_order_type_largestFirst,
			R.string.pull_order_type_largestFirst_description
		),
		FolderPullOrder(
			Constants.FOLDER_PULL_ORDER_SMALLEST_FIRST,
			R.string.pull_order_type_smallestFirst,
			R.string.pull_order_type_smallestFirst_description
		)
	)

	fun getCurrentFolderPullOrder(): FolderPullOrder {
		return folderPullOrders.find { it.value == folder.type }!!
	}

	var editedVersioning: Folder.Versioning? by savedStateHandle.saveable {
		mutableStateOf(null)
	}


	// DIALOGS
	private var discardDialog: Dialog? = null
	var showDiscardDialog by savedStateHandle.saveable { mutableStateOf(false) }
	var showFolderTypeDialog by savedStateHandle.saveable { mutableStateOf(false) }
	var showFolderPullOrderDialog by savedStateHandle.saveable { mutableStateOf(false) }
	var showVersioningDialog by savedStateHandle.saveable { mutableStateOf(false) }
	var showDeleteDialog by savedStateHandle.saveable { mutableStateOf(false) }

	var isValidFolder by savedStateHandle.saveable { mutableStateOf(false) }


	fun setService(service: SyncthingService) {
		serviceReference = WeakReference(service)
	}

	fun setInitialState(
		context: Context,
		onFinish: () -> Unit,
		isCreate: Boolean,
		folderId: String?,
		newDeviceId: String?,
		folderLabel: String?
	) {
		// Prevent resetting state on configuration changes
		if (isInitialized) return
		isInitialized = true

		isCreateMode = isCreate

		if (api != null) {
			if (isCreate) {
				initNewFolder(
					folderId,
					newDeviceId,
					folderLabel
				)
			} else {
				if (folderId == null) {
					Toast.makeText(context, "folderId == null", Toast.LENGTH_LONG).show()
					onFinish()
					return
				}
				loadExistingFolder(folderId, newDeviceId, onFinish, context)
			}
		}

		loadDeviceList()
	}

	fun onLabelChange(value: String) {
		folder = folder.copy(label = value)
		folderNeedsToUpdate(true)
	}

	fun onIdChange(value: String) {
		folder = folder.copy(id = value)
		folderNeedsToUpdate(true)
	}

	fun onPathChange(value: String) {
		folder = folder.copy(path = value)
		folderNeedsToUpdate(true)
	}

	fun onFsWatcherChange(checked: Boolean) {
		folder = folder.copy(fsWatcherEnabled = checked)
		folderNeedsToUpdate(true)
	}

	fun onPausedChange(checked: Boolean) {
		folder = folder.copy(paused = checked)
		folderNeedsToUpdate(true)
	}

	fun onPullOrderChange(order: String) {

		if (
			order != Constants.FOLDER_PULL_ORDER_RANDOM &&
			order != Constants.FOLDER_PULL_ORDER_ALPHABETIC &&
			order != Constants.FOLDER_PULL_ORDER_NEWEST_FIRST &&
			order != Constants.FOLDER_PULL_ORDER_OLDEST_FIRST &&
			order != Constants.FOLDER_PULL_ORDER_LARGEST_FIRST &&
			order != Constants.FOLDER_PULL_ORDER_SMALLEST_FIRST
		) {
			Log.wtf(this.toString(), "Illegal folder pull order")
			return
		}

		folder = folder.copy(order = order)

		folderNeedsToUpdate(true)
	}

	fun onFolderTypeChange(type: String) {
		if (
			type != Constants.FOLDER_TYPE_SEND_RECEIVE &&
			type != Constants.FOLDER_TYPE_SEND_ONLY &&
			type != Constants.FOLDER_TYPE_RECEIVE_ONLY
		) {
			Log.wtf(this.toString(), "Folder type is bad!")
			return
		}

		folder = folder.copy(type = type)
		folderNeedsToUpdate(true)
	}

	fun onVersioningChange(
		type: String? = null,
		param: String? = null,
		paramValue: String? = null
	) {
		val temp = editedVersioning?.deepCopy()
		if (temp != null) {
			temp.type = type
			if (param != null)
				temp.params[param] = paramValue
			editedVersioning = temp.deepCopy()
		}

		Log.d(
			"onVersioningChange",
			"editedVersioning: $editedVersioning\n" +
					"folder.versioning: ${folder.versioning}"
		)
	}

	fun onVersioningSave() {
		val changedVersioning: Folder.Versioning = if (editedVersioning?.type.isNullOrEmpty() || editedVersioning!!.type == Constants.FVER_TYPE_NONE) {
			Folder.Versioning()
		} else {
			editedVersioning!!.deepCopy()
		}
		folder = folder.copy(versioning = changedVersioning)
		folderNeedsToUpdate(true)
		Log.i(
			"onVersioningSave",
			"editedVersioning: $editedVersioning\n" +
					"folder.versioning: ${folder.versioning}"
		)
	}


	fun onCancel(onFinish: () -> Unit) {
		if (folderNeedsToUpdate) {
			showDiscardDialog = true
		} else {
			onFinish()
		}
	}

	fun onSave(context: Context, onFinish: () -> Unit) {
		val currentApi = api

		if (folder.id.isNullOrEmpty()) {
			Toast.makeText(context, R.string.folder_id_required, Toast.LENGTH_LONG)
				.show()
			return
		}
		if (!Folder.isValidId(folder.id)) {
			Toast.makeText(context, R.string.folder_id_not_valid, Toast.LENGTH_LONG)
				.show()
			return
		}
		if (folder.path.isNullOrEmpty()) {
			Toast.makeText(context, R.string.folder_path_required, Toast.LENGTH_LONG)
				.show()
			return
		}
		val dir = File(folder.path!!)

		if (!dir.exists()) {
			val created = dir.mkdirs()
			if (created) {
				Log.v(this.toString(), "Created directory: ${folder.path}")
			} else {
				Log.v("FolderViewModel", "Failed to create directory: ${folder.path}")
				Toast.makeText(
					context, "Failed to create directory: ${folder.path}." +
							"\nYou may ignore this if running as root", Toast.LENGTH_LONG
				).show()
			}
		}

		val markerDir = File(dir, FOLDER_MARKER_NAME)
		if (!markerDir.exists()) {
			if (markerDir.mkdir()) {
				Log.v(this.toString(), "Created new directory: " + markerDir.path)
				File(markerDir, "empty").createNewFile()
			} else {
				Log.v(this.toString(), "Failed to create: " + markerDir.path)
			}
		}

		if (currentApi != null) {
			if (isCreateMode) {
				currentApi.createFolder(folder)
			} else {
				currentApi.updateFolder(folder)
			}
			onFinish()
		} else {
			Toast.makeText(context, R.string.syncthing_disabled, Toast.LENGTH_SHORT)
				.show()
		}
	}

	fun onDelete(onFinish: () -> Unit) {
		api?.removeFolder(folder.id)
		folderNeedsToUpdate(false)
		onFinish()
	}

	fun onDone(context: Context, onFinish: () -> Unit = {}) {
		if (folderNeedsToUpdate) {
			onSave(context, onFinish)
		} else {
			onCancel(onFinish)
		}

	}

	fun onFolderSelectedViaSaf(uri: Uri, context: Context) {
		folderUri = uri

		var targetPath = FileUtils.getAbsolutePathFromSAFUri(context, uri)

		if (targetPath != null) {
			targetPath = Util.formatPath(targetPath)
		}

		if (targetPath.isNullOrEmpty() || targetPath == File.separator) {
			onPathChange(targetPath ?: "")
		} else {
			val cleanPath = FileUtils.cutTrailingSlash(targetPath)

			onPathChange(cleanPath)

			checkWritePermissions(context, cleanPath)
		}
	}

	fun onDeviceSelectionChange(device: Device, isSelected: Boolean) {
		val index = deviceList.indexOfFirst { it.device.deviceID == device.deviceID }
		if (index != -1) {
			deviceList[index] = deviceList[index].copy(isSelected = isSelected)
		}

		val newDevices = folder.devices.map { it.copy() }.toMutableList()
		if (isSelected) {
			newDevices.add(Folder.Device(deviceID = device.deviceID))
		} else {
			newDevices.removeAll { it.deviceID == device.deviceID }
		}
		folder = folder.copy(devices = newDevices)
		folderNeedsToUpdate(true)
	}

	private fun loadDeviceList() {
		val currentApi = api ?: return
		val allDevices = currentApi.getDevices(false) ?: emptyList<Device>()
		deviceList.clear()

		allDevices.forEach { device ->
			val isSelected = folder.devices.any { it.deviceID == device.deviceID }
			deviceList.add(DeviceUiState(device, isSelected))
		}
	}

	private fun checkWritePermissions(context: Context?, path: String?) {
		if (context == null || path.isNullOrEmpty()) return
		canWriteToPath = Util.nativeBinaryCanWriteToPath(context, path)

		// TODO: Mirror original logic to force "Send Only" if read-only

		if (!canWriteToPath) {
			folder.type = Constants.FOLDER_TYPE_SEND_ONLY
			folderNeedsToUpdate(true)
		}
	}

	fun checkPathAccess(path: String?) {
		viewModelScope.launch {
			isPathWritable = withContext(Dispatchers.IO) {
				if (path.isNullOrEmpty()) return@withContext false
				val file = File(path)
				if (file.exists()) {
					file.canWrite() && file.canRead()
				} else {
					val parentDir = file.parentFile
					parentDir != null && parentDir.canWrite() && parentDir.canRead() && checkFileName(file)
				}
			}
		}
	}

	private fun initNewFolder(
		folderId: String?,
		deviceId: String?,
		folderLabel: String?,
		fsWatcherEnabled: Boolean? = null,
		fsWatcherDelayS: Int? = null,
		type: String? = null,
		paused: Boolean? = null
	) {
		val newFolder = Folder()
		newFolder.label = folderLabel
		newFolder.id = folderId ?: generateRandomFolderId()
		newFolder.path = null

		deviceId?.let { newFolder.addDevice(it) }
		if (type != null) newFolder.type = type
		newFolder.fsWatcherEnabled = true
		newFolder.fsWatcherDelayS = 10
		/**
		 * Folder rescan interval defaults to 3600s as it is the default in
		 * syncthing when the file watcher is enabled and a new folder is created.
		 */
		// TODO: Make a setting for default rescan interval and custom rescan interval in folder screen
		newFolder.rescanIntervalS = 3600
		if (paused != null) newFolder.paused = paused
		newFolder.versioning = Folder.Versioning()

		editedVersioning = newFolder.versioning!!.deepCopy()

		folder = newFolder
		checkPathAccess(folder.path)
	}

	private fun loadExistingFolder(
		folderId: String,
		newDeviceId: String?,
		onFinish: () -> Unit,
		context: Context
	) {
		val currentApi = api ?: return
		val folders = currentApi.folders ?: emptyList<Folder>()

		val found = folders.find { it?.id == folderId }
		if (found == null) {
			onDone(context, onFinish)
			return
		}
		checkWritePermissions(serviceReference?.get(), found.path)

		newDeviceId?.let {
			found.addDevice(it)
			folderNeedsToUpdate(true)
		}

		editedVersioning = found.versioning?.deepCopy()
		folder = found
		checkPathAccess(folder.path)
	}

	private fun generateRandomFolderId(): String {
		val chars = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray()
		val chArr = CharArray(11)
		var i = 0
		while (i <= 10) {
			if (i == 5) {
				chArr[5] = '-'
				i++
			}
			val char = chars[Random.nextInt(chars.size)]
			chArr[i] = char
			i++
		}
		return String(chArr)
	}

	fun editIgnores(context: Context) {
		try {
			val ignoreFile = File(folder.path, IGNORE_FILE_NAME)
			if (!ignoreFile.exists() && !ignoreFile.createNewFile()) {
				Toast.makeText(context, R.string.create_ignore_file_error, Toast.LENGTH_SHORT)
					.show()
				return
			}
			val intent = Intent(Intent.ACTION_EDIT)
			val uri = Uri.fromFile(ignoreFile)
			intent.setDataAndType(uri, "text/plain")
			intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

			context.startActivity(intent)
		} catch (e: IOException) {
			Log.w(TAG, e)
		} catch (e: ActivityNotFoundException) {
			Log.w(TAG, e)
			Toast.makeText(context, R.string.edit_ignore_file_error, Toast.LENGTH_SHORT).show()
		}
	}

	private fun updateFolder() {
		if (!isCreateMode) {
			/**
			 * RestApi is guaranteed not to be null as [onServiceStateChange]
			 * immediately finishes this activity if SyncthingService shuts down.
			 */
			api!!.updateFolder(folder)
		}
	}

	fun checkPathAccess(): Boolean {
		if (folder.path == null) {
			return false
		}
		val file = File(folder.path!!)

		return if (file.exists()) {
			file.canWrite() && file.canRead()
		} else {
			val parentDir = file.parentFile
			parentDir != null && parentDir.canWrite() && parentDir.canRead() && checkFileName(file)
		}
	}

	private fun checkFileName(file: File): Boolean {
		return try {
			if (file.createNewFile()) {
				file.delete() // Clean up instantly if successful
				true
			} else {
				false
			}
		} catch (e: IOException) {
			false
		}
	}

	fun prettyFileName(path: String?): String {
		if (path == null) return ""
		val regex = Regex("^/storage/emulated/0")
		return path.replace(regex, "~")
	}

	fun unPrettyFileName(path: String): String {
		val regex = Regex("^~")
		return path.replace(regex, "/storage/emulated/0")
	}


	private fun folderNeedsToUpdate(value: Boolean) {
		folderNeedsToUpdate = value
	}

	companion object {
		const val EXTRA_NOTIFICATION_ID: String =
			"activities.syncthingandroid.benedek.dev.FolderActivity.NOTIFICATION_ID"
		const val EXTRA_IS_CREATE: String =
			"activities.syncthingandroid.benedek.dev.FolderActivity.IS_CREATE"
		const val EXTRA_FOLDER_ID: String =
			"activities.syncthingandroid.benedek.dev.FolderActivity.FOLDER_ID"
		const val EXTRA_FOLDER_LABEL: String =
			"activities.syncthingandroid.benedek.dev.FolderActivity.FOLDER_LABEL"
		const val EXTRA_DEVICE_ID: String =
			"activities.syncthingandroid.benedek.dev.FolderActivity.DEVICE_ID"

		private const val TAG = "FolderActivity"

		private const val IS_SHOWING_DELETE_DIALOG = "DELETE_FOLDER_DIALOG_STATE"
		private const val IS_SHOW_DISCARD_DIALOG = "DISCARD_FOLDER_DIALOG_STATE"

		private const val FILE_VERSIONING_DIALOG_REQUEST = 3454
		private const val PULL_ORDER_DIALOG_REQUEST = 3455
		private const val FOLDER_TYPE_DIALOG_REQUEST = 3456
		private const val CHOOSE_FOLDER_REQUEST = 3459

		const val FOLDER_MARKER_NAME = ".stfolder"
		const val IGNORE_FILE_NAME = ".stignore"
	}
}