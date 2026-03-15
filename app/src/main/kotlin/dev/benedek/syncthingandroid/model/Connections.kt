package dev.benedek.syncthingandroid.model

import kotlin.math.max

class Connections {
    var total: Connection? = null
    var connectionsMap: MutableMap<String?, Connection?>? = null

    class Connection {
        var paused: Boolean = false
        var clientVersion: String? = null
        var at: String? = null
        var connected: Boolean = false
        var inBytesTotal: Long = 0
        var outBytesTotal: Long = 0
        var type: String? = null
        var address: String? = null

        // These fields are not sent from Syncthing, but are populated on the client side.
        var completion: Int = 0
        var inBits: Long = 0
        var outBits: Long = 0

        fun setTransferRate(previous: Connection, msElapsed: Long) {
            val secondsElapsed = msElapsed / 1000
            val inBytes = 8 * (inBytesTotal - previous.inBytesTotal) / secondsElapsed
            val outBytes = 8 * (outBytesTotal - previous.outBytesTotal) / secondsElapsed
            inBits = max(0, inBytes)
            outBits = max(0, outBytes)
        }
    }
}
