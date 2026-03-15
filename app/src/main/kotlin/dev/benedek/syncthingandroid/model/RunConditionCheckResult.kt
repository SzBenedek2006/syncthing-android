package dev.benedek.syncthingandroid.model

import dev.benedek.syncthingandroid.R
import java.util.Collections

class RunConditionCheckResult(blockReasons: MutableList<BlockerReason?>) {
    enum class BlockerReason(@JvmField val resId: Int) {
        ON_BATTERY(R.string.syncthing_disabled_reason_on_battery),
        ON_CHARGER(R.string.syncthing_disabled_reason_on_charger),
        POWERSAVING_ENABLED(R.string.syncthing_disabled_reason_powersaving),
        GLOBAL_SYNC_DISABLED(R.string.syncthing_disabled_reason_android_sync_disabled),
        WIFI_SSID_NOT_WHITELISTED(R.string.syncthing_disabled_reason_wifi_ssid_not_whitelisted),
        WIFI_WIFI_IS_METERED(R.string.syncthing_disabled_reason_wifi_is_metered),
        NO_NETWORK_OR_FLIGHTMODE(R.string.syncthing_disabled_reason_no_network_or_flightmode),
        NO_MOBILE_CONNECTION(R.string.syncthing_disabled_reason_no_mobile_connection),
        NO_WIFI_CONNECTION(R.string.syncthing_disabled_reason_no_wifi_connection),
        NO_ALLOWED_NETWORK(R.string.syncthing_disabled_reason_no_allowed_method)
    }

    val isShouldRun: Boolean = blockReasons.isEmpty()
    val blockReasons: MutableList<BlockerReason?> = Collections.unmodifiableList<BlockerReason?>(blockReasons)

    /**
     * Use SHOULD_RUN instead.
     * Note: of course anybody could still construct it by providing an empty list to the other
     * constructor.
     */
    private constructor() : this(mutableListOf<BlockerReason?>())


    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false

        val that = o as RunConditionCheckResult

        if (this.isShouldRun != that.isShouldRun) return false
        return this.blockReasons == that.blockReasons
    }

    override fun hashCode(): Int {
        var result = (if (this.isShouldRun) 1 else 0)
        result = 31 * result + blockReasons.hashCode()
        return result
    }

    companion object {
        val SHOULD_RUN: RunConditionCheckResult = RunConditionCheckResult()
    }
}
