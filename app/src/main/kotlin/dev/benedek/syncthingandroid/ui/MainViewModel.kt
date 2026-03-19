@file:Suppress("LocalVariableName", "PrivatePropertyName")

package dev.benedek.syncthingandroid.ui

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
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
import dev.benedek.syncthingandroid.model.SystemVersion
import dev.benedek.syncthingandroid.service.RestApi
import dev.benedek.syncthingandroid.service.SyncthingService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference


class MainViewModel : ViewModel() {
    val mainVisibilityObserver = MainVisibilityObserver()

    private var serviceReference: WeakReference<SyncthingService>? = null
    val api: RestApi? get() = serviceReference?.get()?.api
    var isApiReady by mutableStateOf(false)
        private set


    var systemInfo by mutableStateOf<SystemInfo?>(null)
    var systemVersion by mutableStateOf<SystemVersion?>(null)

    var announceTotal: Int = 0
    var announceConnected: Int = 0

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

    private val DEVICES_COMPARATOR =
        Comparator { lhs: Device?, rhs: Device? -> lhs!!.name.compareTo(rhs!!.name) }

    fun setService(service: SyncthingService) {
        serviceReference = WeakReference(service)
    }

    class MainVisibilityObserver : DefaultLifecycleObserver {
        var isVisible: Boolean = true
            private set
        var apiRefreshDelay: Long = 10L
        var apiCallDelay: Long = 100L
        var refreshInterval: Long = 2000L

        override fun onResume(owner: LifecycleOwner) {
            super.onResume(owner)

            apiRefreshDelay = 10L
            apiCallDelay = 100L
            refreshInterval = 1000L

            isVisible = true
        }

        override fun onPause(owner: LifecycleOwner) {
            super.onPause(owner)

            apiRefreshDelay = 100L
            apiCallDelay = 1000L
            refreshInterval = 10000L
        }

        override fun onStop(owner: LifecycleOwner) {
            super.onStop(owner)

            isVisible = false
        }
    }



    init {
        viewModelScope.launch {
            while (true) {
                val _isApiReady = (api != null)

                if (isApiReady != _isApiReady) {
                    if (!isApiReady) delay(mainVisibilityObserver.apiCallDelay*4) // UPDATE for the number of api calls each cycle to avoid empty list error
                    isApiReady = _isApiReady
                }

                delay(if (_isApiReady) 333L else 33L)
            }
        }
        fetchSystemData()
    }


    fun fetchSystemData() {
        viewModelScope.launch {
            while (isActive) {
                while (!isApiReady) {
                    delay(mainVisibilityObserver.apiRefreshDelay)
                }
                api?.getSystemInfo { info ->
                    if (info != null) {
                        systemInfo = info
                        announceTotal = systemInfo?.discoveryMethods ?: 0
                        announceConnected = announceTotal - (systemInfo?.discoveryErrors?.size ?: 0)
                    }
                }
                delay(mainVisibilityObserver.apiCallDelay)

                updateFolderStatuses()
                delay(mainVisibilityObserver.apiCallDelay)

                val _devices = api?.getDevices(false)
                _devices?.sortWith(DEVICES_COMPARATOR)
                devices = _devices
                delay(mainVisibilityObserver.apiCallDelay)

                api?.getSystemVersion { version ->
                    version?.let { systemVersion = it }
                } ?: {
                    systemVersion = SystemVersion()
                    systemVersion!!.version = "Syncthing is not running."
                }
                delay(mainVisibilityObserver.apiCallDelay)

                api?.getConnections { conn ->
                    if (conn != null) {
                        deviceStatuses = conn
                    }
                }
                do {
                    delay(mainVisibilityObserver.refreshInterval)
                } while (!mainVisibilityObserver.isVisible)


            }
        }
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
                    pixels[offset + x] = if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
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

            api?.getFolderStatus(folderId) {returnedId, status ->
                folderStatuses.update { currentMap ->
                    currentMap + ((returnedId ?: "") to (status ?: FolderStatus())) // FIXME: Find a more robust way of doing this
                }
            }

        }
    }
}