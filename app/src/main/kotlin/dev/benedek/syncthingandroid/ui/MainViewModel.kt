package dev.benedek.syncthingandroid.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dev.benedek.syncthingandroid.model.Connections
import dev.benedek.syncthingandroid.model.SystemInfo
import dev.benedek.syncthingandroid.model.SystemVersion
import dev.benedek.syncthingandroid.service.RestApi
import dev.benedek.syncthingandroid.service.SyncthingService
import java.lang.ref.WeakReference

class MainViewModel : ViewModel() {
    private var api: RestApi? = null
    private var serviceReference: WeakReference<SyncthingService>? = null



    var systemInfo by mutableStateOf<SystemInfo?>(null)
    var systemVersion by mutableStateOf<SystemVersion?>(null)
    var connections by mutableStateOf<Connections?>(null)

    val announceTotal = systemInfo?.discoveryMethods ?: 0
    val announceConnected = announceTotal - (systemInfo?.discoveryErrors?.size ?: 0)

    // DIALOGS
    var showDeviceIdDialog by mutableStateOf(false)
    var showRestartDialog by mutableStateOf(false)
    var showExitDialog by mutableStateOf(false)

    fun setService(service: SyncthingService) {
        serviceReference = WeakReference(service)
        api = service.api
    }


    fun fetchSystemData() {
        api?.getSystemInfo { info ->
            if (info != null) {
                systemInfo = info // Compose automatically reacts to this change
            }
        }

        api?.getSystemVersion { version ->
            if (version != null) {
                systemVersion = version
            }
        }

        api?.getConnections { conn ->
            if (conn != null) {
                connections = conn
            }
        }
    }
}