@file:Suppress("unused")

package dev.benedek.syncthingandroid.ui

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
import androidx.lifecycle.ViewModel
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.model.Device
import dev.benedek.syncthingandroid.model.Folder
import dev.benedek.syncthingandroid.model.isValid
import dev.benedek.syncthingandroid.service.Constants
import dev.benedek.syncthingandroid.service.RestApi
import dev.benedek.syncthingandroid.service.SyncthingService
import dev.benedek.syncthingandroid.util.FileUtils
import dev.benedek.syncthingandroid.util.Util
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import kotlin.random.Random

class FolderViewModel : ViewModel() {
    private var serviceReference: WeakReference<SyncthingService>? = null
    private var api: RestApi? = null
    var folder by mutableStateOf(Folder(), policy = neverEqualPolicy())
        private set
    var folderUri: Uri by mutableStateOf(Uri.EMPTY)

    var isCreateMode by mutableStateOf(false)
    var canWriteToPath by mutableStateOf(false)
        private set
    data class DeviceUiState(val device: Device, val isSelected: Boolean)
    var deviceList = mutableStateListOf<DeviceUiState>()
        private set
    private var folderNeedsToUpdate by mutableStateOf(false)
    private var versioning: Folder.Versioning? = null
    private var isInitialized = false

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

    var editedVersioning: Folder.Versioning? by mutableStateOf(null, policy = neverEqualPolicy())


    private var discardDialog: Dialog? = null
    var showDiscardDialog by mutableStateOf(false)
    var showFolderTypeDialog by mutableStateOf(false)
    var showFolderPullOrderDialog by mutableStateOf(false)
    var showVersioningDialog by mutableStateOf(false)
    var showDeleteDialog by mutableStateOf(false)

    var isValidFolder by mutableStateOf(false)


    fun setService(boundService: SyncthingService) {
        serviceReference = WeakReference(boundService)
        api = boundService.api
    }

    fun setInitialState(
        context: Context,
        onFinish: () -> Unit,
        isCreate: Boolean,
        folderId: String?,
        deviceId: String?,
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
                    deviceId,
                    folderLabel
                )
            } else {
                if (folderId == null) {
                    Toast.makeText(context, "folderId == null", Toast.LENGTH_LONG).show()
                    onFinish()
                    return
                }
                loadExistingFolder(folderId, onFinish, context)
            }
        }

        loadDeviceList()
    }

    fun onLabelChange(value: String) {
        folder.label = value
        folder = folder
        folderNeedsToUpdate = true
    }
    fun onIdChange(value: String) {
        folder.id = value
        folder = folder
        folderNeedsToUpdate = true
    }
    fun onPathChange(value: String) {
        folder.path = value
        folder = folder
        folderNeedsToUpdate = true
    }
    fun onFsWatcherChange(checked: Boolean) {
        folder.fsWatcherEnabled = checked
        folder = folder
        folderNeedsToUpdate = true
    }
    fun onPausedChange(checked: Boolean) {
        folder.paused = checked
        folder = folder
        folderNeedsToUpdate = true
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

        folder.order = order
        folder = folder
        folderNeedsToUpdate = true
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

        folder.type = type
        folder = folder
        folderNeedsToUpdate = true
    }
    fun onVersioningChange(type: String? = null, param: String? = null, paramValue: String? = null) {
        editedVersioning!!.type = type
        if (param != null)
            editedVersioning!!.params[param] = paramValue

        editedVersioning.also { editedVersioning = it } // This is sadly needed for compose to update!
        Log.d(
            "onVersioningChange",
            "editedVersioning: $editedVersioning\n" +
                "folder.versioning: ${folder.versioning}")
    }

    fun onVersioningSave() {
        if (editedVersioning!!.type.isNullOrEmpty() || editedVersioning!!.type == Constants.FVER_TYPE_NONE) {
            folder.versioning = Folder.Versioning()
        } else {
            folder.versioning = editedVersioning!!.deepCopy()
        }
        folder = folder
        folderNeedsToUpdate = true
        Log.d(
            "onVersioningSave",
            "editedVersioning: $editedVersioning\n" +
                    "folder.versioning: ${folder.versioning}")
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
        if (!folder.id!!.isValid) {
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
        folderNeedsToUpdate = false
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

        if (isSelected) {
            folder.addDevice(device.deviceID)
        } else {
            folder.removeDevice(device.deviceID)
        }
        folder = folder // Needed for compose to update :<
        folderNeedsToUpdate = true
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
        folder.label = folderLabel
        folder.id = folderId ?: generateRandomFolderId()
        folder.path = null
        folder.addDevice(deviceId)
        if (type != null) folder.type = type
        folder.fsWatcherEnabled = true
        folder.fsWatcherDelayS = 10
        /**
         * Folder rescan interval defaults to 3600s as it is the default in
         * syncthing when the file watcher is enabled and a new folder is created.
         */
        // TODO: Make a setting for default rescan interval and custom rescan interval in folder screen
        folder.rescanIntervalS = 3600

        if (paused != null) folder.paused = paused

        folder.versioning = Folder.Versioning()
        editedVersioning = folder.versioning!!.deepCopy()
    }

    private fun loadExistingFolder(folderId: String, onFinish: () -> Unit, context: Context) {
        val currentApi = api ?: return
        val folders = currentApi.folders ?: emptyList<Folder>()

        val found = folders.find { it.id == folderId }
        if (found == null) {
            onDone(context, onFinish)
            return
        }
        folder = found
        checkWritePermissions(serviceReference?.get(), found.path)


        editedVersioning = folder.versioning!!.deepCopy()
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
                Toast.makeText(context, R.string.create_ignore_file_error, Toast.LENGTH_SHORT).show()
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