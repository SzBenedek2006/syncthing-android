package dev.benedek.syncthingandroid.model

class Config {
    var version: Int = 0
    var devices: MutableList<Device?>? = null
    var folders: MutableList<Folder?>? = null
    var gui: Gui? = null
    var options: Options? = null
    var remoteIgnoredDevices: MutableList<RemoteIgnoredDevice?>? = null

    class Gui {
        var enabled: Boolean = false
        var address: String? = null
        var user: String? = null
        var password: String? = null
        var useTLS: Boolean = false
        var apiKey: String? = null
        var insecureAdminAccess: Boolean = false
        var theme: String? = null
    }
}
