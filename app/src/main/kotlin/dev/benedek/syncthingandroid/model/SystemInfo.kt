package dev.benedek.syncthingandroid.model

class SystemInfo {
    var alloc: Long = 0
    var cpuPercent: Double = 0.0
    var goroutines: Int = 0
    var myID: String? = null
    // sys = ram?
    var sys: Long = 0
    var discoveryEnabled: Boolean = false
    var discoveryMethods: Int = 0
    var discoveryErrors: MutableMap<String, String>? = null
    var urVersionMax: Int = 0
}
