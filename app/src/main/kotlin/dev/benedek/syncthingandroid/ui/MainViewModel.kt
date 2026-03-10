package dev.benedek.syncthingandroid.ui

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import dev.benedek.syncthingandroid.model.Connections
import dev.benedek.syncthingandroid.model.SystemInfo
import dev.benedek.syncthingandroid.model.SystemVersion
import dev.benedek.syncthingandroid.service.RestApi
import dev.benedek.syncthingandroid.service.SyncthingService
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class MainViewModel : ViewModel() {
    private var serviceReference: WeakReference<SyncthingService>? = null
    val api: RestApi? get() = serviceReference?.get()?.api



    var systemInfo by mutableStateOf<SystemInfo?>(null)
    var systemVersion by mutableStateOf<SystemVersion?>(null)
    var connections by mutableStateOf<Connections?>(null)

    var announceTotal: Int = 0
    var announceConnected: Int = 0

    // DIALOGS
    var showDeviceIdDialog by mutableStateOf(false)
    var showRestartDialog by mutableStateOf(false)
    var showExitDialog by mutableStateOf(false)

    fun setService(service: SyncthingService) {
        serviceReference = WeakReference(service)
    }


    init {
        fetchSystemData()
    }


    fun fetchSystemData() {
        viewModelScope.launch {
            while (isActive) {
                api?.getSystemInfo { info ->
                    if (info != null) {
                        systemInfo = info
                        announceTotal = systemInfo?.discoveryMethods ?: 0
                        announceConnected = announceTotal - (systemInfo?.discoveryErrors?.size ?: 0)
                    }
                }
                delay(100)

                if (api != null) api!!.getSystemVersion { version ->
                    if (version != null) {
                        systemVersion = version
                    }
                } else {
                    systemVersion = SystemVersion()
                    systemVersion!!.version = "Syncthing not running"
                }
                delay(100)

                api?.getConnections { conn ->
                    if (conn != null) {
                        connections = conn
                    }
                }
                delay(2000)


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
                    pixels[offset + x] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
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
}