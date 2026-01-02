package dev.benedek.syncthingandroid.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.fragments.SettingsFragment
import dev.benedek.syncthingandroid.service.Constants
import dev.benedek.syncthingandroid.service.SyncthingService
import dev.benedek.syncthingandroid.util.Util

class SettingsActivity : SyncthingActivity(),
    PreferenceFragmentCompat.OnPreferenceStartScreenCallback {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preferences)
        setTitle(R.string.settings_title)

        val settingsFragment = SettingsFragment()
        val bundle = Bundle()
        bundle.putString(
            EXTRA_OPEN_SUB_PREF_SCREEN, intent.getStringExtra(
                EXTRA_OPEN_SUB_PREF_SCREEN
            )
        )
        settingsFragment.setArguments(bundle)
        supportFragmentManager.beginTransaction()
            .replace(R.id.settings_container, settingsFragment)
            .commit()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.PermissionRequestType.LOCATION.ordinal) {
            var granted = grantResults.isNotEmpty()
            for (i in grantResults.indices) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    granted = false
                    break
                }
            }
            if (granted) {
                this.startService(
                    Intent(this, SyncthingService::class.java)
                        .setAction(SyncthingService.ACTION_REFRESH_NETWORK_INFO)
                )
            } else {
                Util.getAlertDialogBuilder(this)
                    .setTitle(R.string.sync_only_wifi_ssids_location_permission_rejected_dialog_title)
                    .setMessage(R.string.sync_only_wifi_ssids_location_permission_rejected_dialog_content)
                    .setPositiveButton(android.R.string.ok, null).show()
            }
        }
    }

    override fun onPreferenceStartScreen(
        caller: PreferenceFragmentCompat,
        screen: PreferenceScreen
    ): Boolean {
        val fragment = SettingsFragment()
        val args = Bundle()
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, screen.key)
        fragment.setArguments(args)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, fragment)
            .addToBackStack(screen.key)
            .commit()

        return true
    }

    companion object {
        const val EXTRA_OPEN_SUB_PREF_SCREEN: String =
            "activities.syncthingandroid.benedek.dev.SettingsActivity.OPEN_SUB_PREF_SCREEN"
    }
}