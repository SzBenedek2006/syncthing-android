package dev.benedek.syncthingandroid.activities

import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.TaskStackBuilder
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreferenceCompat
import com.google.common.base.Joiner
import com.google.common.base.Splitter
import com.google.common.collect.Iterables
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.SyncthingApp
import dev.benedek.syncthingandroid.model.Config
import dev.benedek.syncthingandroid.model.Options
import dev.benedek.syncthingandroid.model.SystemInfo
import dev.benedek.syncthingandroid.service.Constants
import dev.benedek.syncthingandroid.service.NotificationHandler
import dev.benedek.syncthingandroid.service.RestApi
import dev.benedek.syncthingandroid.service.SyncthingService
import dev.benedek.syncthingandroid.util.Languages
import dev.benedek.syncthingandroid.util.Util
import dev.benedek.syncthingandroid.views.WifiSsidPreference
import eu.chainfire.libsuperuser.Shell
import java.lang.ref.WeakReference
import java.security.InvalidParameterException
import java.util.HashSet
import javax.inject.Inject

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

    class SettingsFragment : PreferenceFragmentCompat(), OnServiceConnectedListener,
        SyncthingService.OnServiceStateChangeListener, Preference.OnPreferenceChangeListener,
        Preference.OnPreferenceClickListener {
        @JvmField
        @Inject
        var mNotificationHandler: NotificationHandler? = null

        @JvmField
        @Inject
        var mPreferences: SharedPreferences? = null

        private var mCategoryRunConditions: PreferenceGroup? = null
        private var mRunConditions: SwitchPreferenceCompat? = null
        private var mStartServiceOnBoot: SwitchPreferenceCompat? = null
        private var mPowerSource: ListPreference? = null
        private var mRunOnMobileData: SwitchPreferenceCompat? = null
        private var mRunOnWifi: SwitchPreferenceCompat? = null
        private var mRunOnMeteredWifi: SwitchPreferenceCompat? = null
        private var mWifiSsidWhitelist: WifiSsidPreference? = null
        private var mRunInFlightMode: SwitchPreferenceCompat? = null

        private var mCategorySyncthingOptions: Preference? = null
        private var mDeviceName: EditTextPreference? = null
        private var mListenAddresses: EditTextPreference? = null
        private var mMaxRecvKbps: EditTextPreference? = null
        private var mMaxSendKbps: EditTextPreference? = null
        private var mNatEnabled: SwitchPreferenceCompat? = null
        private var mLocalAnnounceEnabled: SwitchPreferenceCompat? = null
        private var mGlobalAnnounceEnabled: SwitchPreferenceCompat? = null
        private var mRelaysEnabled: SwitchPreferenceCompat? = null
        private var mGlobalAnnounceServers: EditTextPreference? = null
        private var mAddress: EditTextPreference? = null
        private var mUrAccepted: SwitchPreferenceCompat? = null

        private var mCategoryBackup: Preference? = null

        /* Experimental options */
        private var mUseRoot: SwitchPreferenceCompat? = null
        private var mUseWakelock: SwitchPreferenceCompat? = null
        private var mUseTor: SwitchPreferenceCompat? = null
        private var mSocksProxyAddress: EditTextPreference? = null
        private var mHttpProxyAddress: EditTextPreference? = null

        private var mSyncthingVersion: Preference? = null

        private var mSyncthingService: SyncthingService? = null
        private var mApi: RestApi? = null

        private var mOptions: Options? = null
        private var mGui: Config.Gui? = null

        private var mPendingConfig = false

        /**
         * Indicates if run conditions were changed and need to be
         * re-evaluated when the user leaves the preferences screen.
         */
        private var mPendingRunConditions = false

        override fun onCreate(savedInstanceState: Bundle?) {
            (requireActivity().application as SyncthingApp).component().inject(this)
            super.onCreate(savedInstanceState)
            (activity as SyncthingActivity).registerOnServiceConnectedListener(this)
        }

        /**
         * Loads layout, sets version from Rest API.
         *
         * Manual target API as we manually check if ActionBar is available (for ActionBar back button).
         */
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.app_settings, rootKey)
            val screen = preferenceScreen
            mRunConditions =
                findPreference(Constants.PREF_RUN_CONDITIONS)
            mStartServiceOnBoot =
                findPreference(Constants.PREF_START_SERVICE_ON_BOOT)
            mPowerSource =
                findPreference(Constants.PREF_POWER_SOURCE)
            mRunOnMobileData =
                findPreference(Constants.PREF_RUN_ON_MOBILE_DATA)
            mRunOnWifi =
                findPreference(Constants.PREF_RUN_ON_WIFI)
            if (mRunOnWifi == null) {
                Log.d("null", "mRunOnWifi: FUCK!")
            }
            mRunOnMeteredWifi =
                findPreference(Constants.PREF_RUN_ON_METERED_WIFI)
            mWifiSsidWhitelist =
                findPreference(Constants.PREF_WIFI_SSID_WHITELIST)
            mRunInFlightMode =
                findPreference(Constants.PREF_RUN_IN_FLIGHT_MODE)

            val languagePref = findPreference<ListPreference?>(Languages.PREFERENCE_LANGUAGE)
            val categoryBehaviour = findPreference<PreferenceGroup?>("category_behaviour")
            if (Build.VERSION.SDK_INT >= 24) {
                if (languagePref != null) {
                    categoryBehaviour!!.removePreference(languagePref)
                }
            } else {
                val languages = Languages(activity)
                languagePref!!.setDefaultValue(Languages.USE_SYSTEM_DEFAULT)
                languagePref.entries = languages.allNames
                languagePref.entryValues = languages.getSupportedLocales()
                languagePref.onPreferenceChangeListener =
                    Preference.OnPreferenceChangeListener { _: Preference?, o: Any? ->
                        languages.forceChangeLanguage(activity, o as String?)
                        false
                    }
            }

            mDeviceName = findPreference("deviceName")
            mListenAddresses = findPreference("listenAddresses")
            mMaxRecvKbps = findPreference("maxRecvKbps")
            mMaxSendKbps = findPreference("maxSendKbps")
            mNatEnabled = findPreference("natEnabled")
            mLocalAnnounceEnabled = findPreference("localAnnounceEnabled")
            mGlobalAnnounceEnabled =
                findPreference("globalAnnounceEnabled")
            mRelaysEnabled = findPreference("relaysEnabled")
            mGlobalAnnounceServers = findPreference("globalAnnounceServers")
            mAddress = findPreference("address")
            mUrAccepted = findPreference("urAccepted")

            mCategoryBackup = findPreference("category_backup")
            val exportConfig = findPreference<Preference?>("export_config")
            val importConfig = findPreference<Preference?>("import_config")

            val undoIgnoredDevicesFolders = findPreference<Preference?>(
                KEY_UNDO_IGNORED_DEVICES_FOLDERS
            )
            val debugFacilitiesEnabled =
                findPreference<Preference?>(Constants.PREF_DEBUG_FACILITIES_ENABLED)
            val environmentVariables = findPreference<Preference?>("environment_variables")
            val stResetDatabase = findPreference<Preference?>("st_reset_database")
            val stResetDeltas = findPreference<Preference?>("st_reset_deltas")

            mUseRoot = findPreference(Constants.PREF_USE_ROOT)
            mUseWakelock = findPreference(Constants.PREF_USE_WAKE_LOCK)
            mUseTor = findPreference(Constants.PREF_USE_TOR)
            mSocksProxyAddress =
                findPreference(Constants.PREF_SOCKS_PROXY_ADDRESS)
            mHttpProxyAddress =
                findPreference(Constants.PREF_HTTP_PROXY_ADDRESS)

            mSyncthingVersion = findPreference("syncthing_version")
            val appVersion = findPreference<Preference?>("app_version")

            mRunOnMeteredWifi!!.isEnabled = mRunOnWifi!!.isChecked
            mWifiSsidWhitelist!!.isEnabled = mRunOnWifi!!.isChecked

            mCategorySyncthingOptions = findPreference("category_syncthing_options")
            setPreferenceCategoryChangeListener(
                mCategorySyncthingOptions
            ) { preference: Preference?, o: Any? ->
                this.onSyncthingPreferenceChange(
                    preference!!,
                    o
                )
            }
            mCategoryRunConditions = findPreference("category_run_conditions")
            setPreferenceCategoryChangeListener(
                mCategoryRunConditions
            ) { preference: Preference?, o: Any? ->
                this.onRunConditionPreferenceChange(
                    preference!!,
                    o!!
                )
            }

            if (!mRunConditions!!.isChecked) {
                for (index in 1..<mCategoryRunConditions!!.preferenceCount) {
                    mCategoryRunConditions!!.getPreference(index).isEnabled = false
                }
            }

            exportConfig!!.onPreferenceClickListener = this
            importConfig!!.onPreferenceClickListener = this

            undoIgnoredDevicesFolders!!.onPreferenceClickListener = this
            debugFacilitiesEnabled!!.onPreferenceChangeListener = this
            environmentVariables!!.onPreferenceChangeListener = this
            stResetDatabase!!.onPreferenceClickListener = this
            stResetDeltas!!.onPreferenceClickListener = this

            /* Experimental options */
            mUseRoot!!.onPreferenceClickListener = this
            mUseWakelock!!.onPreferenceChangeListener = this
            mUseTor!!.onPreferenceChangeListener = this

            mSocksProxyAddress!!.isEnabled = (!mUseTor!!.isChecked as Boolean?)!!
            mSocksProxyAddress!!.onPreferenceChangeListener = this
            mHttpProxyAddress!!.isEnabled = (!mUseTor!!.isChecked as Boolean?)!!
            mHttpProxyAddress!!.onPreferenceChangeListener = this

            /* Initialize summaries */
            screen.findPreference<Preference?>(Constants.PREF_POWER_SOURCE)!!
                .setSummary(mPowerSource!!.getEntry())
            val wifiSsidSummary = TextUtils.join(
                ", ", mPreferences!!.getStringSet(
                    Constants.PREF_WIFI_SSID_WHITELIST,
                    HashSet()
                )!!
            )
            screen.findPreference<Preference?>(Constants.PREF_WIFI_SSID_WHITELIST)!!.setSummary(
                if (TextUtils.isEmpty(wifiSsidSummary)) getString(R.string.run_on_all_wifi_networks) else getString(
                    R.string.run_on_whitelisted_wifi_networks,
                    wifiSsidSummary
                )
            )
            handleSocksProxyPreferenceChange(
                screen.findPreference(Constants.PREF_SOCKS_PROXY_ADDRESS)!!,
                mPreferences!!.getString(
                    Constants.PREF_SOCKS_PROXY_ADDRESS,
                    ""
                )!!
            )
            handleHttpProxyPreferenceChange(
                screen.findPreference(Constants.PREF_HTTP_PROXY_ADDRESS)!!,
                mPreferences!!.getString(
                    Constants.PREF_HTTP_PROXY_ADDRESS,
                    ""
                )!!
            )

            val themePreference = findPreference<ListPreference?>(Constants.PREF_APP_THEME)
            themePreference!!.onPreferenceChangeListener = this

            try {
                appVersion!!.setSummary(
                    requireActivity().packageManager
                        .getPackageInfo(requireActivity().packageName, 0).versionName
                )
            } catch (_: PackageManager.NameNotFoundException) {
                Log.d(TAG, "Failed to get app version name")
            }

            openSubPrefScreen()
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            // Targeting android 15 enables and 16 forces edge-to-edge,
            ViewCompat.setOnApplyWindowInsetsListener(
                view.getRootView()
            ) { v: View?, windowInsets: WindowInsetsCompat? ->
                val insets = windowInsets!!.getInsets(WindowInsetsCompat.Type.systemBars())
                val mlp = v!!.layoutParams as ViewGroup.MarginLayoutParams
                mlp.leftMargin = insets.left
                mlp.bottomMargin = insets.bottom
                mlp.rightMargin = insets.right
                v.setLayoutParams(mlp)
                WindowInsetsCompat.CONSUMED
            }

            ViewCompat.setOnApplyWindowInsetsListener(
                view.getRootView()
            ) { v: View?, insets: WindowInsetsCompat? ->
                val bars = insets!!.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            or WindowInsetsCompat.Type.displayCutout()
                )
                v!!.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                WindowInsetsCompat.CONSUMED
            }
        }

        private fun openSubPrefScreen() {
            val bundle = arguments ?: return
            val openSubPrefScreen = bundle.getString(EXTRA_OPEN_SUB_PREF_SCREEN, "")
            // Open sub preferences screen if EXTRA_OPEN_SUB_PREF_SCREEN was passed in bundle.
            if (openSubPrefScreen != null && !TextUtils.isEmpty(openSubPrefScreen)) {
                Log.v(TAG, "Transitioning to pref screen $openSubPrefScreen")
                val targetScreen = findPreference<Preference?>(openSubPrefScreen)
                if (targetScreen != null) {
                    // Programmatically "clicks" the preference, letting the fragment handle the navigation.
                    onPreferenceTreeClick(targetScreen)
                }
            }
        }

        override fun onServiceConnected() {
            Log.v(TAG, "onServiceConnected")
            if (activity == null) return

            mSyncthingService = (activity as SyncthingActivity).service
            mSyncthingService!!.registerOnServiceStateChangeListener(this)
        }

        override fun onServiceStateChange(currentState: SyncthingService.State?) {
            mApi = mSyncthingService!!.api
            val isSyncthingRunning = (mApi != null) &&
                    mApi!!.isConfigLoaded &&
                    (currentState == SyncthingService.State.ACTIVE)
            mCategorySyncthingOptions!!.isEnabled = isSyncthingRunning
            mCategoryBackup!!.isEnabled = isSyncthingRunning

            if (!isSyncthingRunning) return

            mSyncthingVersion!!.setSummary(mApi!!.version)
            mOptions = mApi!!.options
            mGui = mApi!!.gui

            val joiner = Joiner.on(", ")
            mDeviceName!!.setText(mApi!!.getLocalDevice().name)
            mListenAddresses!!.setText(joiner.join(mOptions!!.listenAddresses))
            mMaxRecvKbps!!.setText(mOptions!!.maxRecvKbps.toString())
            mMaxSendKbps!!.setText(mOptions!!.maxSendKbps.toString())
            mNatEnabled!!.setChecked(mOptions!!.natEnabled)
            mLocalAnnounceEnabled!!.setChecked(mOptions!!.localAnnounceEnabled)
            mGlobalAnnounceEnabled!!.setChecked(mOptions!!.globalAnnounceEnabled)
            mRelaysEnabled!!.setChecked(mOptions!!.relaysEnabled)
            mGlobalAnnounceServers!!.setText(joiner.join(mOptions!!.globalAnnounceServers))
            mAddress!!.setText(mGui!!.address)
            mApi!!.getSystemInfo { systemInfo: SystemInfo? ->
                mUrAccepted!!.setChecked(
                    mOptions!!.isUsageReportingAccepted(systemInfo!!.urVersionMax)
                )
            }
        }

        override fun onDestroy() {
            if (mSyncthingService != null) {
                mSyncthingService!!.unregisterOnServiceStateChangeListener(this)
            }
            super.onDestroy()
        }

        private fun setPreferenceCategoryChangeListener(
            category: Preference?, listener: Preference.OnPreferenceChangeListener?
        ) {
            val pg = category as PreferenceGroup
            for (i in 0..<pg.preferenceCount) {
                val p = pg.getPreference(i)
                p.onPreferenceChangeListener = listener
            }
        }

        fun onRunConditionPreferenceChange(preference: Preference, o: Any): Boolean {
            when (preference.key) {
                Constants.PREF_RUN_CONDITIONS -> {
                    val enabled = o as Boolean

                    /* Just for reference, a beautiful for loop was here, instead of this disgusting kotlin code.
                    * for (int index = 1; index < mCategoryRunConditions.getPreferenceCount(); ++index) {
                    *    mCategoryRunConditions.getPreference(index).setEnabled(enabled);
                    } */
                    var index = 1 // Also, this was automatically made to val
                    while (index < mCategoryRunConditions!!.preferenceCount) {
                        mCategoryRunConditions!!.getPreference(index).isEnabled = enabled
                        ++index
                    }

                    if (enabled) {
                        mRunOnMeteredWifi!!.isEnabled = mRunOnWifi!!.isChecked
                        mWifiSsidWhitelist!!.isEnabled = mRunOnWifi!!.isChecked
                    }
                }

                Constants.PREF_POWER_SOURCE -> {
                    mPowerSource!!.setValue(o.toString())
                    preference.setSummary(mPowerSource!!.getEntry())
                }

                Constants.PREF_RUN_ON_WIFI -> {
                    mRunOnMeteredWifi!!.isEnabled = (o as Boolean?)!!
                    mWifiSsidWhitelist!!.isEnabled = o
                }

                Constants.PREF_WIFI_SSID_WHITELIST -> {
                    val wifiSsidSummary = TextUtils.join(
                        ", ",
                        (o as MutableSet<*>)
                    )
                    preference.setSummary(
                        if (TextUtils.isEmpty(wifiSsidSummary)) getString(R.string.run_on_all_wifi_networks) else getString(
                            R.string.run_on_whitelisted_wifi_networks,
                            wifiSsidSummary
                        )
                    )
                }
            }
            mPendingRunConditions = true
            return true
        }

        fun onSyncthingPreferenceChange(preference: Preference, o: Any?): Boolean {
            val splitter = Splitter.on(",").trimResults().omitEmptyStrings()
            when (preference.key) {
                "deviceName" -> {
                    val localDevice = mApi!!.getLocalDevice()
                    localDevice.name = o as String?
                    mApi!!.editDevice(localDevice)
                }

                "listenAddresses" -> mOptions!!.listenAddresses = Iterables.toArray<String?>(
                    splitter.split((o as String?)!!),
                    String::class.java
                )

                "maxRecvKbps" -> {
                    var maxRecvKbps: Int
                    try {
                        maxRecvKbps = (o as String?)!!.toInt()
                    } catch (_: Exception) {
                        Toast.makeText(
                            activity,
                            resources.getString(
                                R.string.invalid_integer_value,
                                0,
                                Int.MAX_VALUE
                            ),
                            Toast.LENGTH_LONG
                        )
                            .show()
                        return false
                    }
                    mOptions!!.maxRecvKbps = maxRecvKbps
                }

                "maxSendKbps" -> {
                    var maxSendKbps: Int
                    try {
                        maxSendKbps = (o as String?)!!.toInt()
                    } catch (_: Exception) {
                        Toast.makeText(
                            activity,
                            resources.getString(
                                R.string.invalid_integer_value,
                                0,
                                Int.MAX_VALUE
                            ),
                            Toast.LENGTH_LONG
                        )
                            .show()
                        return false
                    }
                    mOptions!!.maxSendKbps = maxSendKbps
                }

                "natEnabled" -> mOptions!!.natEnabled = o as Boolean
                "localAnnounceEnabled" -> mOptions!!.localAnnounceEnabled = o as Boolean
                "globalAnnounceEnabled" -> mOptions!!.globalAnnounceEnabled = o as Boolean
                "relaysEnabled" -> mOptions!!.relaysEnabled = o as Boolean
                "globalAnnounceServers" -> mOptions!!.globalAnnounceServers =
                    Iterables.toArray<String?>(
                        splitter.split((o as String?)!!),
                        String::class.java
                    )

                "address" -> mGui!!.address = o as String?
                "urAccepted" -> mApi!!.getSystemInfo { systemInfo: SystemInfo? ->
                    mOptions!!.urAccepted = if (o as Boolean)
                        systemInfo!!.urVersionMax
                    else
                        Options.USAGE_REPORTING_DENIED
                }

                else -> throw InvalidParameterException()
            }

            mApi!!.editSettings(mGui, mOptions)
            mPendingConfig = true
            return true
        }

        override fun onStop() {
            if (mSyncthingService != null) {
                mNotificationHandler!!.updatePersistentNotification(mSyncthingService)
                if (mPendingConfig) {
                    if (mApi != null &&
                        mSyncthingService!!.currentState != SyncthingService.State.DISABLED
                    ) {
                        mApi!!.saveConfigAndRestart()
                        mPendingConfig = false
                    }
                }
                if (mPendingRunConditions) {
                    mSyncthingService!!.evaluateRunConditions()
                }
            }
            super.onStop()
        }

        /**
         * Sends the updated value to [RestApi], and sets it as the summary
         * for EditTextPreference.
         */
        override fun onPreferenceChange(preference: Preference, o: Any): Boolean {
            when (preference.key) {
                Constants.PREF_DEBUG_FACILITIES_ENABLED -> mPendingConfig = true
                Constants.PREF_ENVIRONMENT_VARIABLES -> if ((o as String).matches("^(\\w+=[\\w:/.]+)?( \\w+=[\\w:/.]+)*$".toRegex())) {
                    mPendingConfig = true
                } else {
                    Toast.makeText(
                        activity,
                        R.string.toast_invalid_environment_variables,
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    return false
                }

                Constants.PREF_USE_WAKE_LOCK -> mPendingConfig = true
                Constants.PREF_USE_TOR -> {
                    mSocksProxyAddress!!.isEnabled = !(o as Boolean)
                    mHttpProxyAddress!!.isEnabled = !(o as Boolean)
                    mPendingConfig = true
                }

                Constants.PREF_SOCKS_PROXY_ADDRESS -> {
                    if (o.toString().trim { it <= ' ' } == mPreferences!!.getString(
                            Constants.PREF_SOCKS_PROXY_ADDRESS,
                            ""
                        )) return false
                    if (handleSocksProxyPreferenceChange(
                            preference,
                            o.toString().trim { it <= ' ' })
                    ) {
                        mPendingConfig = true
                    } else {
                        return false
                    }
                }

                Constants.PREF_HTTP_PROXY_ADDRESS -> {
                    if (o.toString().trim { it <= ' ' } == mPreferences!!.getString(
                            Constants.PREF_HTTP_PROXY_ADDRESS,
                            ""
                        )) return false
                    if (handleHttpProxyPreferenceChange(
                            preference,
                            o.toString().trim { it <= ' ' })
                    ) {
                        mPendingConfig = true
                    } else {
                        return false
                    }
                }

                Constants.PREF_APP_THEME ->                     // Recreate activities with the correct colors
                    TaskStackBuilder.create(requireActivity())
                        .addNextIntent(Intent(activity, MainActivity::class.java))
                        .addNextIntent(requireActivity().intent)
                        .startActivities()
            }

            return true
        }

        override fun onPreferenceClick(preference: Preference): Boolean {
            val intent: Intent
            when (preference.key) {
                Constants.PREF_USE_ROOT -> {
                    if (mUseRoot!!.isChecked) {
                        // Only check preference after root was granted.
                        mUseRoot!!.setChecked(false)
                        TestRootTask(this).execute()
                    } else {
                        Thread { Util.fixAppDataPermissions(activity) }.start()
                        mPendingConfig = true
                    }
                    return true
                }

                KEY_EXPORT_CONFIG -> {
                    Util.getAlertDialogBuilder(activity)
                        .setMessage(R.string.dialog_confirm_export)
                        .setPositiveButton(
                            android.R.string.ok
                        ) { _: DialogInterface?, _: Int ->
                            mSyncthingService!!.exportConfig()
                            Toast.makeText(
                                activity,
                                getString(
                                    R.string.config_export_successful,
                                    Constants.EXPORT_PATH
                                ), Toast.LENGTH_LONG
                            ).show()
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
                    return true
                }

                KEY_IMPORT_CONFIG -> {
                    Util.getAlertDialogBuilder(activity)
                        .setMessage(R.string.dialog_confirm_import)
                        .setPositiveButton(
                            android.R.string.ok
                        ) { _: DialogInterface?, _: Int ->
                            if (mSyncthingService!!.importConfig()) {
                                Toast.makeText(
                                    activity,
                                    getString(R.string.config_imported_successful),
                                    Toast.LENGTH_SHORT
                                ).show()
                                // No need to restart, as we shutdown to import the config, and
                                // then have to start Syncthing again.
                            } else {
                                Toast.makeText(
                                    activity,
                                    getString(
                                        R.string.config_import_failed,
                                        Constants.EXPORT_PATH
                                    ), Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
                    return true
                }

                KEY_UNDO_IGNORED_DEVICES_FOLDERS -> {
                    Util.getAlertDialogBuilder(activity)
                        .setMessage(R.string.undo_ignored_devices_folders_question)
                        .setPositiveButton(
                            android.R.string.ok
                        ) { _: DialogInterface?, _: Int ->
                            if (mApi == null) {
                                Toast.makeText(
                                    activity,
                                    getString(R.string.generic_error) + getString(R.string.syncthing_disabled_title),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {

                                mApi!!.undoIgnoredDevicesAndFolders()
                                mPendingConfig = true
                                Toast.makeText(
                                    activity,
                                    getString(R.string.undo_ignored_devices_folders_done),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
                    return true
                }

                KEY_ST_RESET_DATABASE -> {
                    intent = Intent(activity, SyncthingService::class.java)
                        .setAction(SyncthingService.ACTION_RESET_DATABASE)

                    Util.getAlertDialogBuilder(activity)
                        .setTitle(R.string.st_reset_database_title)
                        .setMessage(R.string.st_reset_database_question)
                        .setPositiveButton(
                            android.R.string.ok
                        ) { _: DialogInterface?, _: Int ->
                            requireActivity().startService(intent)
                            Toast.makeText(
                                activity,
                                R.string.st_reset_database_done,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        .setNegativeButton(
                            android.R.string.cancel
                        ) { _: DialogInterface?, _: Int -> }
                        .show()
                    return true
                }

                KEY_ST_RESET_DELTAS -> {
                    intent = Intent(activity, SyncthingService::class.java)
                        .setAction(SyncthingService.ACTION_RESET_DELTAS)

                    Util.getAlertDialogBuilder(activity)
                        .setTitle(R.string.st_reset_deltas_title)
                        .setMessage(R.string.st_reset_deltas_question)
                        .setPositiveButton(
                            android.R.string.ok
                        ) { _: DialogInterface?, _: Int ->
                            requireActivity().startService(intent)
                            Toast.makeText(
                                activity,
                                R.string.st_reset_deltas_done,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        .setNegativeButton(
                            android.R.string.cancel
                        ) { _: DialogInterface?, _: Int -> }
                        .show()
                    return true
                }

                else -> return false
            }
        }

        /**
         * Enables or disables [.mUseRoot] preference depending whether root is available.
         */
        private class TestRootTask(context: SettingsFragment?) :
            AsyncTask<Void?, Void?, Boolean?>() {
            private val refSettingsFragment: WeakReference<SettingsFragment?> =
                WeakReference<SettingsFragment?>(context)

            @Deprecated("Deprecated in Java")
            override fun doInBackground(vararg params: Void?): Boolean {
                return Shell.SU.available()
            }

            @Deprecated("Deprecated in Java")
            override fun onPostExecute(haveRoot: Boolean?) {
                // Get a reference to the fragment if it is still there.
                val settingsFragment = refSettingsFragment.get() ?: return
                if (haveRoot == true) {
                    settingsFragment.mPendingConfig = true
                    settingsFragment.mUseRoot!!.setChecked(true)
                } else {
                    Toast.makeText(
                        settingsFragment.activity,
                        R.string.toast_root_denied,
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            }
        }

        /**
         * Handles a new user input for the SOCKS proxy preference.
         * Returns if the changed setting requires a restart.
         */
        private fun handleSocksProxyPreferenceChange(
            preference: Preference,
            newValue: String
        ): Boolean {
            // Valid input is either a proxy address or an empty field to disable the proxy.
            if (newValue == "") {
                preference.setSummary(
                    getString(R.string.do_not_use_proxy) + " " + getString(R.string.generic_example) + ": " + getString(
                        R.string.socks_proxy_address_example
                    )
                )
                return true
            } else if (newValue.matches("^socks5://.*:\\d{1,5}$".toRegex())) {
                preference.setSummary(getString(R.string.use_proxy) + " " + newValue)
                return true
            } else {
                Toast.makeText(
                    activity,
                    R.string.toast_invalid_socks_proxy_address,
                    Toast.LENGTH_SHORT
                )
                    .show()
                return false
            }
        }

        /**
         * Handles a new user input for the HTTP(S) proxy preference.
         * Returns if the changed setting requires a restart.
         */
        private fun handleHttpProxyPreferenceChange(
            preference: Preference,
            newValue: String
        ): Boolean {
            // Valid input is either a proxy address or an empty field to disable the proxy.
            if (newValue == "") {
                preference.setSummary(
                    getString(R.string.do_not_use_proxy) + " " + getString(R.string.generic_example) + ": " + getString(
                        R.string.http_proxy_address_example
                    )
                )
                return true
            } else if (newValue.matches("^http://.*:\\d{1,5}$".toRegex())) {
                preference.setSummary(getString(R.string.use_proxy) + " " + newValue)
                return true
            } else {
                Toast.makeText(
                    activity,
                    R.string.toast_invalid_http_proxy_address,
                    Toast.LENGTH_SHORT
                )
                    .show()
                return false
            }
        }

        companion object {
            private const val TAG = "SettingsFragment"
            private const val KEY_EXPORT_CONFIG = "export_config"
            private const val KEY_IMPORT_CONFIG = "import_config"
            private const val KEY_UNDO_IGNORED_DEVICES_FOLDERS = "undo_ignored_devices_folders"
            private const val KEY_ST_RESET_DATABASE = "st_reset_database"
            private const val KEY_ST_RESET_DELTAS = "st_reset_deltas"
        }
    }

    companion object {
        const val EXTRA_OPEN_SUB_PREF_SCREEN: String =
            "activities.syncthingandroid.nutomic.dev.SettingsActivity.OPEN_SUB_PREF_SCREEN"
    }
}