package dev.benedek.syncthingandroid.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.TaskStackBuilder;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.SwitchPreferenceCompat;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

import java.lang.ref.WeakReference;
import java.security.InvalidParameterException;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import dev.benedek.syncthingandroid.R;
import dev.benedek.syncthingandroid.SyncthingApp;
import dev.benedek.syncthingandroid.model.Config;
import dev.benedek.syncthingandroid.model.Device;
import dev.benedek.syncthingandroid.model.Options;
import dev.benedek.syncthingandroid.service.Constants;
import dev.benedek.syncthingandroid.service.NotificationHandler;
import dev.benedek.syncthingandroid.service.RestApi;
import dev.benedek.syncthingandroid.service.SyncthingService;
import dev.benedek.syncthingandroid.util.Languages;
import dev.benedek.syncthingandroid.util.Util;
import dev.benedek.syncthingandroid.views.WifiSsidPreference;
import eu.chainfire.libsuperuser.Shell;
import kotlin.OverloadResolutionByLambdaReturnType;

public class SettingsActivity extends SyncthingActivity implements PreferenceFragmentCompat.OnPreferenceStartScreenCallback {


    public static final String EXTRA_OPEN_SUB_PREF_SCREEN =
            "activities.syncthingandroid.nutomic.dev.SettingsActivity.OPEN_SUB_PREF_SCREEN";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);
        setTitle(R.string.settings_title);

        SettingsFragment settingsFragment = new SettingsFragment();
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_OPEN_SUB_PREF_SCREEN, getIntent().getStringExtra(EXTRA_OPEN_SUB_PREF_SCREEN));
        settingsFragment.setArguments(bundle);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings_container, settingsFragment)
                .commit();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Constants.PermissionRequestType.LOCATION.ordinal()) {
            boolean granted = grantResults.length > 0;
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }
            if (granted) {
                this.startService(new Intent(this, SyncthingService.class)
                        .setAction(SyncthingService.ACTION_REFRESH_NETWORK_INFO));
            } else {
                Util.getAlertDialogBuilder(this)
                        .setTitle(R.string.sync_only_wifi_ssids_location_permission_rejected_dialog_title)
                        .setMessage(R.string.sync_only_wifi_ssids_location_permission_rejected_dialog_content)
                        .setPositiveButton(android.R.string.ok, null).show();
            }
        }
    }

    @Override
    public boolean onPreferenceStartScreen(PreferenceFragmentCompat caller, PreferenceScreen screen) {
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, screen.getKey());
        fragment.setArguments(args);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_container, fragment)
                .addToBackStack(screen.getKey())
                .commit();

        return true;
    }

    public static class SettingsFragment extends PreferenceFragmentCompat
            implements SyncthingActivity.OnServiceConnectedListener,
            SyncthingService.OnServiceStateChangeListener, Preference.OnPreferenceChangeListener,
            Preference.OnPreferenceClickListener {


        private static final String TAG = "SettingsFragment";
        private static final String KEY_EXPORT_CONFIG = "export_config";
        private static final String KEY_IMPORT_CONFIG = "import_config";
        private static final String KEY_UNDO_IGNORED_DEVICES_FOLDERS = "undo_ignored_devices_folders";
        private static final String KEY_ST_RESET_DATABASE = "st_reset_database";
        private static final String KEY_ST_RESET_DELTAS = "st_reset_deltas";

        @Inject NotificationHandler mNotificationHandler;
        @Inject SharedPreferences mPreferences;

        private PreferenceGroup    mCategoryRunConditions;
        private SwitchPreferenceCompat mRunConditions;
        private SwitchPreferenceCompat mStartServiceOnBoot;
        private ListPreference     mPowerSource;
        private SwitchPreferenceCompat mRunOnMobileData;
        private SwitchPreferenceCompat mRunOnWifi;
        private SwitchPreferenceCompat mRunOnMeteredWifi;
        private WifiSsidPreference mWifiSsidWhitelist;
        private SwitchPreferenceCompat mRunInFlightMode;

        private Preference         mCategorySyncthingOptions;
        private EditTextPreference mDeviceName;
        private EditTextPreference mListenAddresses;
        private EditTextPreference mMaxRecvKbps;
        private EditTextPreference mMaxSendKbps;
        private SwitchPreferenceCompat mNatEnabled;
        private SwitchPreferenceCompat mLocalAnnounceEnabled;
        private SwitchPreferenceCompat mGlobalAnnounceEnabled;
        private SwitchPreferenceCompat mRelaysEnabled;
        private EditTextPreference mGlobalAnnounceServers;
        private EditTextPreference mAddress;
        private SwitchPreferenceCompat mUrAccepted;

        private Preference mCategoryBackup;

        /* Experimental options */
        private SwitchPreferenceCompat mUseRoot;
        private SwitchPreferenceCompat mUseWakelock;
        private SwitchPreferenceCompat mUseTor;
        private EditTextPreference mSocksProxyAddress;
        private EditTextPreference mHttpProxyAddress;

        private Preference mSyncthingVersion;

        private SyncthingService mSyncthingService;
        private RestApi mApi;

        private Options mOptions;
        private Config.Gui mGui;

        private Boolean mPendingConfig = false;

        /**
         * Indicates if run conditions were changed and need to be
         * re-evaluated when the user leaves the preferences screen.
         */
        private Boolean mPendingRunConditions = false;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            ((SyncthingApp) getActivity().getApplication()).component().inject(this);
            super.onCreate(savedInstanceState);
            ((SyncthingActivity) getActivity()).registerOnServiceConnectedListener(this);
        }

        /**
         * Loads layout, sets version from Rest API.
         *
         * Manual target API as we manually check if ActionBar is available (for ActionBar back button).
         */

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

            setPreferencesFromResource(R.xml.app_settings, rootKey);
            PreferenceScreen screen = getPreferenceScreen();
            mRunConditions =
                    findPreference(Constants.PREF_RUN_CONDITIONS);
            mStartServiceOnBoot =
                    findPreference(Constants.PREF_START_SERVICE_ON_BOOT);
            mPowerSource =
                    findPreference(Constants.PREF_POWER_SOURCE);
            mRunOnMobileData =
                    findPreference(Constants.PREF_RUN_ON_MOBILE_DATA);
            mRunOnWifi =
                    findPreference(Constants.PREF_RUN_ON_WIFI);
            if (mRunOnWifi == null) {
                Log.d("null", "mRunOnWifi: FUCK!");
            }
            mRunOnMeteredWifi =
                    findPreference(Constants.PREF_RUN_ON_METERED_WIFI);
            mWifiSsidWhitelist =
                    findPreference(Constants.PREF_WIFI_SSID_WHITELIST);
            mRunInFlightMode =
                    findPreference(Constants.PREF_RUN_IN_FLIGHT_MODE);

            ListPreference languagePref = findPreference(Languages.PREFERENCE_LANGUAGE);
            PreferenceGroup categoryBehaviour = findPreference("category_behaviour");
            if (Build.VERSION.SDK_INT >= 24) {
                if (languagePref != null) {
                    categoryBehaviour.removePreference(languagePref);
                }
            } else {
                Languages languages = new Languages(getActivity());
                languagePref.setDefaultValue(Languages.USE_SYSTEM_DEFAULT);
                languagePref.setEntries(languages.getAllNames());
                languagePref.setEntryValues(languages.getSupportedLocales());
                languagePref.setOnPreferenceChangeListener((p, o) -> {
                    languages.forceChangeLanguage(getActivity(), (String) o);
                    return false;
                });
            }

            mDeviceName             = findPreference("deviceName");
            mListenAddresses        = findPreference("listenAddresses");
            mMaxRecvKbps            = findPreference("maxRecvKbps");
            mMaxSendKbps            = findPreference("maxSendKbps");
            mNatEnabled             = findPreference("natEnabled");
            mLocalAnnounceEnabled   = findPreference("localAnnounceEnabled");
            mGlobalAnnounceEnabled  = findPreference("globalAnnounceEnabled");
            mRelaysEnabled          = findPreference("relaysEnabled");
            mGlobalAnnounceServers  = findPreference("globalAnnounceServers");
            mAddress                = findPreference("address");
            mUrAccepted             = findPreference("urAccepted");

            mCategoryBackup         = findPreference("category_backup");
            Preference exportConfig = findPreference("export_config");
            Preference importConfig = findPreference("import_config");

            Preference undoIgnoredDevicesFolders    = findPreference(KEY_UNDO_IGNORED_DEVICES_FOLDERS);
            Preference debugFacilitiesEnabled       = findPreference(Constants.PREF_DEBUG_FACILITIES_ENABLED);
            Preference environmentVariables         = findPreference("environment_variables");
            Preference stResetDatabase              = findPreference("st_reset_database");
            Preference stResetDeltas                = findPreference("st_reset_deltas");

            mUseRoot                        = findPreference(Constants.PREF_USE_ROOT);
            mUseWakelock                    = findPreference(Constants.PREF_USE_WAKE_LOCK);
            mUseTor                         = findPreference(Constants.PREF_USE_TOR);
            mSocksProxyAddress              = findPreference(Constants.PREF_SOCKS_PROXY_ADDRESS);
            mHttpProxyAddress               = findPreference(Constants.PREF_HTTP_PROXY_ADDRESS);

            mSyncthingVersion       = findPreference("syncthing_version");
            Preference appVersion   = findPreference("app_version");

            mRunOnMeteredWifi.setEnabled(mRunOnWifi.isChecked());
            mWifiSsidWhitelist.setEnabled(mRunOnWifi.isChecked());

            mCategorySyncthingOptions = findPreference("category_syncthing_options");
            setPreferenceCategoryChangeListener(mCategorySyncthingOptions, this::onSyncthingPreferenceChange);
            mCategoryRunConditions = findPreference("category_run_conditions");
            setPreferenceCategoryChangeListener(mCategoryRunConditions, this::onRunConditionPreferenceChange);

            if (!mRunConditions.isChecked()) {
                for (int index = 1; index < mCategoryRunConditions.getPreferenceCount(); ++index) {
                    mCategoryRunConditions.getPreference(index).setEnabled(false);
                }
            }

            exportConfig.setOnPreferenceClickListener(this);
            importConfig.setOnPreferenceClickListener(this);

            undoIgnoredDevicesFolders.setOnPreferenceClickListener(this);
            debugFacilitiesEnabled.setOnPreferenceChangeListener(this);
            environmentVariables.setOnPreferenceChangeListener(this);
            stResetDatabase.setOnPreferenceClickListener(this);
            stResetDeltas.setOnPreferenceClickListener(this);

            /* Experimental options */
            mUseRoot.setOnPreferenceClickListener(this);
            mUseWakelock.setOnPreferenceChangeListener(this);
            mUseTor.setOnPreferenceChangeListener(this);

            mSocksProxyAddress.setEnabled(!(Boolean) mUseTor.isChecked());
            mSocksProxyAddress.setOnPreferenceChangeListener(this);
            mHttpProxyAddress.setEnabled(!(Boolean) mUseTor.isChecked());
            mHttpProxyAddress.setOnPreferenceChangeListener(this);

            /* Initialize summaries */
            screen.findPreference(Constants.PREF_POWER_SOURCE).setSummary(mPowerSource.getEntry());
            String wifiSsidSummary = TextUtils.join(", ", mPreferences.getStringSet(Constants.PREF_WIFI_SSID_WHITELIST, new HashSet<>()));
            screen.findPreference(Constants.PREF_WIFI_SSID_WHITELIST).setSummary(TextUtils.isEmpty(wifiSsidSummary) ?
                    getString(R.string.run_on_all_wifi_networks) :
                    getString(R.string.run_on_whitelisted_wifi_networks, wifiSsidSummary)
            );
            handleSocksProxyPreferenceChange(screen.findPreference(Constants.PREF_SOCKS_PROXY_ADDRESS),  mPreferences.getString(Constants.PREF_SOCKS_PROXY_ADDRESS, ""));
            handleHttpProxyPreferenceChange(screen.findPreference(Constants.PREF_HTTP_PROXY_ADDRESS), mPreferences.getString(Constants.PREF_HTTP_PROXY_ADDRESS, ""));

            ListPreference themePreference = findPreference(Constants.PREF_APP_THEME);
            themePreference.setOnPreferenceChangeListener(this);

            try {
                appVersion.setSummary(getActivity().getPackageManager()
                        .getPackageInfo(getActivity().getPackageName(), 0).versionName);
            } catch (PackageManager.NameNotFoundException e) {
                Log.d(TAG, "Failed to get app version name");
            }

            openSubPrefScreen(screen);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            // Targeting android 15 enables and 16 forces edge-to-edge,
            ViewCompat.setOnApplyWindowInsetsListener(view.getRootView(), (v, windowInsets) -> {
                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                mlp.leftMargin = insets.left;
                mlp.bottomMargin = insets.bottom;
                mlp.rightMargin = insets.right;
                v.setLayoutParams(mlp);

                return WindowInsetsCompat.CONSUMED;
            });

            ViewCompat.setOnApplyWindowInsetsListener(view.getRootView(), (v, insets) -> {
                Insets bars = insets.getInsets(
                        WindowInsetsCompat.Type.systemBars()
                                | WindowInsetsCompat.Type.displayCutout()
                );
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return WindowInsetsCompat.CONSUMED;
            });
        }

        private void openSubPrefScreen(PreferenceScreen prefScreen) {
            Bundle bundle = getArguments();
            if (bundle == null) {
                return;
            }
            String openSubPrefScreen = bundle.getString(EXTRA_OPEN_SUB_PREF_SCREEN, "");
            // Open sub preferences screen if EXTRA_OPEN_SUB_PREF_SCREEN was passed in bundle.
            if (openSubPrefScreen != null && !TextUtils.isEmpty(openSubPrefScreen)) {
                Log.v(TAG, "Transitioning to pref screen " + openSubPrefScreen);
                final Preference targetScreen = findPreference(openSubPrefScreen);
                if (targetScreen != null) {
                    // Programmatically "clicks" the preference, letting the fragment handle the navigation.
                    onPreferenceTreeClick(targetScreen);
                }
            }
        }

        @Override
        public void onServiceConnected() {
            Log.v(TAG, "onServiceConnected");
            if (getActivity() == null)
                return;

            mSyncthingService = ((SyncthingActivity) getActivity()).getService();
            mSyncthingService.registerOnServiceStateChangeListener(this);
        }

        @Override
        public void onServiceStateChange(SyncthingService.State currentState) {
            mApi = mSyncthingService.getApi();
            boolean isSyncthingRunning = (mApi != null) &&
                mApi.isConfigLoaded() &&
                (currentState == SyncthingService.State.ACTIVE);
            mCategorySyncthingOptions.setEnabled(isSyncthingRunning);
            mCategoryBackup.setEnabled(isSyncthingRunning);

            if (!isSyncthingRunning)
                return;

            mSyncthingVersion.setSummary(mApi.getVersion());
            mOptions = mApi.getOptions();
            mGui = mApi.getGui();

            Joiner joiner = Joiner.on(", ");
            mDeviceName.setText(mApi.getLocalDevice().name);
            mListenAddresses.setText(joiner.join(mOptions.listenAddresses));
            mMaxRecvKbps.setText(Integer.toString(mOptions.maxRecvKbps));
            mMaxSendKbps.setText(Integer.toString(mOptions.maxSendKbps));
            mNatEnabled.setChecked(mOptions.natEnabled);
            mLocalAnnounceEnabled.setChecked(mOptions.localAnnounceEnabled);
            mGlobalAnnounceEnabled.setChecked(mOptions.globalAnnounceEnabled);
            mRelaysEnabled.setChecked(mOptions.relaysEnabled);
            mGlobalAnnounceServers.setText(joiner.join(mOptions.globalAnnounceServers));
            mAddress.setText(mGui.address);
            mApi.getSystemInfo(systemInfo ->
                    mUrAccepted.setChecked(mOptions.isUsageReportingAccepted(systemInfo.urVersionMax)));
        }

        @Override
        public void onDestroy() {
            if (mSyncthingService != null) {
                mSyncthingService.unregisterOnServiceStateChangeListener(this);
            }
            super.onDestroy();
        }

        private void setPreferenceCategoryChangeListener(
                Preference category, Preference.OnPreferenceChangeListener listener) {
            PreferenceGroup pg = (PreferenceGroup) category;
            for (int i = 0; i < pg.getPreferenceCount(); i++) {
                Preference p = pg.getPreference(i);
                p.setOnPreferenceChangeListener(listener);
            }
        }

        public boolean onRunConditionPreferenceChange(Preference preference, Object o) {
            switch (preference.getKey()) {
                case Constants.PREF_RUN_CONDITIONS:
                    boolean enabled = (Boolean) o;
                    for (int index = 1; index < mCategoryRunConditions.getPreferenceCount(); ++index) {
                        mCategoryRunConditions.getPreference(index).setEnabled(enabled);
                    }
                    if (enabled) {
                        mRunOnMeteredWifi.setEnabled(mRunOnWifi.isChecked());
                        mWifiSsidWhitelist.setEnabled(mRunOnWifi.isChecked());
                    }
                    break;
                case Constants.PREF_POWER_SOURCE:
                    mPowerSource.setValue(o.toString());
                    preference.setSummary(mPowerSource.getEntry());
                    break;
                case Constants.PREF_RUN_ON_WIFI:
                    mRunOnMeteredWifi.setEnabled((Boolean) o);
                    mWifiSsidWhitelist.setEnabled((Boolean) o);
                    break;
                case Constants.PREF_WIFI_SSID_WHITELIST:
                    String wifiSsidSummary = TextUtils.join(", ", (Set<String>) o);
                    preference.setSummary(TextUtils.isEmpty(wifiSsidSummary) ?
                        getString(R.string.run_on_all_wifi_networks) :
                        getString(R.string.run_on_whitelisted_wifi_networks, wifiSsidSummary)
                    );
                    break;
            }
            mPendingRunConditions = true;
            return true;
        }

        public boolean onSyncthingPreferenceChange(Preference preference, Object o) {
            Splitter splitter = Splitter.on(",").trimResults().omitEmptyStrings();
            switch (preference.getKey()) {
                case "deviceName":
                    Device localDevice = mApi.getLocalDevice();
                    localDevice.name = (String) o;
                    mApi.editDevice(localDevice);
                    break;
                case "listenAddresses":
                    mOptions.listenAddresses = Iterables.toArray(splitter.split((String) o), String.class);
                    break;
                case "maxRecvKbps":
                    int maxRecvKbps = 0;
                    try {
                        maxRecvKbps = Integer.parseInt((String) o);
                    } catch (Exception e) {
                        Toast.makeText(getActivity(), getResources().getString(R.string.invalid_integer_value, 0, Integer.MAX_VALUE), Toast.LENGTH_LONG)
                                .show();
                        return false;
                    }
                    mOptions.maxRecvKbps = maxRecvKbps;
                    break;
                case "maxSendKbps":
                    int maxSendKbps = 0;
                    try {
                        maxSendKbps = Integer.parseInt((String) o);
                    } catch (Exception e) {
                        Toast.makeText(getActivity(), getResources().getString(R.string.invalid_integer_value, 0, Integer.MAX_VALUE), Toast.LENGTH_LONG)
                                .show();
                        return false;
                    }
                    mOptions.maxSendKbps = maxSendKbps;
                    break;
                case "natEnabled":
                    mOptions.natEnabled = (boolean) o;
                    break;
                case "localAnnounceEnabled":
                    mOptions.localAnnounceEnabled = (boolean) o;
                    break;
                case "globalAnnounceEnabled":
                    mOptions.globalAnnounceEnabled = (boolean) o;
                    break;
                case "relaysEnabled":
                    mOptions.relaysEnabled = (boolean) o;
                    break;
                case "globalAnnounceServers":
                    mOptions.globalAnnounceServers = Iterables.toArray(splitter.split((String) o), String.class);
                    break;
                case "address":
                    mGui.address = (String) o;
                    break;
                case "urAccepted":
                    mApi.getSystemInfo(systemInfo -> {
                        mOptions.urAccepted = ((boolean) o)
                                ? systemInfo.urVersionMax
                                : Options.USAGE_REPORTING_DENIED;
                    });
                    break;
                default: throw new InvalidParameterException();
            }

            mApi.editSettings(mGui, mOptions);
            mPendingConfig = true;
            return true;
        }

        @Override
        public void onStop() {
            if (mSyncthingService != null) {
                mNotificationHandler.updatePersistentNotification(mSyncthingService);
                if (mPendingConfig) {
                    if (mApi != null &&
                            mSyncthingService.getCurrentState() != SyncthingService.State.DISABLED) {
                        mApi.saveConfigAndRestart();
                        mPendingConfig = false;
                    }
                }
                if (mPendingRunConditions) {
                    mSyncthingService.evaluateRunConditions();
                }
            }
            super.onStop();
        }

        /**
         * Sends the updated value to {@link RestApi}, and sets it as the summary
         * for EditTextPreference.
         */
        @Override
        public boolean onPreferenceChange(Preference preference, Object o) {
            switch (preference.getKey()) {
                case Constants.PREF_DEBUG_FACILITIES_ENABLED:
                    mPendingConfig = true;
                    break;
                case Constants.PREF_ENVIRONMENT_VARIABLES:
                    if (((String) o).matches("^(\\w+=[\\w:/\\.]+)?( \\w+=[\\w:/\\.]+)*$")) {
                        mPendingConfig = true;
                    }
                    else {
                        Toast.makeText(getActivity(), R.string.toast_invalid_environment_variables, Toast.LENGTH_SHORT)
                                .show();
                        return false;
                    }
                    break;
                case Constants.PREF_USE_WAKE_LOCK:
                    mPendingConfig = true;
                    break;
                case Constants.PREF_USE_TOR:
                    mSocksProxyAddress.setEnabled(!(Boolean) o);
                    mHttpProxyAddress.setEnabled(!(Boolean) o);
                    mPendingConfig = true;
                    break;
                case Constants.PREF_SOCKS_PROXY_ADDRESS:
                    if (o.toString().trim().equals(mPreferences.getString(Constants.PREF_SOCKS_PROXY_ADDRESS, "")))
                        return false;
                    if (handleSocksProxyPreferenceChange(preference, o.toString().trim())) {
                        mPendingConfig = true;
                    } else {
                        return false;
                    }
                    break;
                case Constants.PREF_HTTP_PROXY_ADDRESS:
                    if (o.toString().trim().equals(mPreferences.getString(Constants.PREF_HTTP_PROXY_ADDRESS, "")))
                        return false;
                    if (handleHttpProxyPreferenceChange(preference, o.toString().trim())) {
                        mPendingConfig = true;
                    } else {
                        return false;
                    }
                    break;
                case Constants.PREF_APP_THEME:
                    // Recreate activities with the correct colors
                    TaskStackBuilder.create(getActivity())
                            .addNextIntent(new Intent(getActivity(), MainActivity.class))
                            .addNextIntent(getActivity().getIntent())
                            .startActivities();
                    break;
            }

            return true;
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            final Intent intent;
            switch (preference.getKey()) {
                case Constants.PREF_USE_ROOT:
                    if (mUseRoot.isChecked()) {
                        // Only check preference after root was granted.
                        mUseRoot.setChecked(false);
                        new TestRootTask(this).execute();
                    } else {
                        new Thread(() -> Util.fixAppDataPermissions(getActivity())).start();
                        mPendingConfig = true;
                    }
                    return true;
                case KEY_EXPORT_CONFIG:
                    Util.getAlertDialogBuilder(getActivity())
                            .setMessage(R.string.dialog_confirm_export)
                            .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                                        mSyncthingService.exportConfig();
                                        Toast.makeText(getActivity(),
                                                getString(R.string.config_export_successful,
                                                Constants.EXPORT_PATH), Toast.LENGTH_LONG).show();
                                    })
                            .setNegativeButton(android.R.string.no, null)
                            .show();
                    return true;
                case KEY_IMPORT_CONFIG:
                    Util.getAlertDialogBuilder(getActivity())
                            .setMessage(R.string.dialog_confirm_import)
                            .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                                        if (mSyncthingService.importConfig()) {
                                            Toast.makeText(getActivity(),
                                                    getString(R.string.config_imported_successful),
                                                    Toast.LENGTH_SHORT).show();
                                            // No need to restart, as we shutdown to import the config, and
                                            // then have to start Syncthing again.
                                        } else {
                                            Toast.makeText(getActivity(),
                                                    getString(R.string.config_import_failed,
                                                    Constants.EXPORT_PATH), Toast.LENGTH_LONG).show();
                                        }
                                    })
                            .setNegativeButton(android.R.string.no, null)
                            .show();
                    return true;
                case KEY_UNDO_IGNORED_DEVICES_FOLDERS:
                    Util.getAlertDialogBuilder(getActivity())
                            .setMessage(R.string.undo_ignored_devices_folders_question)
                            .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                                if (mApi == null) {
                                    Toast.makeText(getActivity(),
                                            getString(R.string.generic_error) + getString(R.string.syncthing_disabled_title),
                                            Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                mApi.undoIgnoredDevicesAndFolders();
                                mPendingConfig = true;
                                Toast.makeText(getActivity(),
                                        getString(R.string.undo_ignored_devices_folders_done),
                                        Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton(android.R.string.no, null)
                            .show();
                    return true;
                case KEY_ST_RESET_DATABASE:
                    intent = new Intent(getActivity(), SyncthingService.class)
                            .setAction(SyncthingService.ACTION_RESET_DATABASE);

                    Util.getAlertDialogBuilder(getActivity())
                            .setTitle(R.string.st_reset_database_title)
                            .setMessage(R.string.st_reset_database_question)
                            .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                                getActivity().startService(intent);
                                Toast.makeText(getActivity(), R.string.st_reset_database_done, Toast.LENGTH_LONG).show();
                            })
                            .setNegativeButton(android.R.string.no, (dialogInterface, i) -> {
                            })
                            .show();
                    return true;
                case KEY_ST_RESET_DELTAS:
                    intent = new Intent(getActivity(), SyncthingService.class)
                            .setAction(SyncthingService.ACTION_RESET_DELTAS);

                    Util.getAlertDialogBuilder(getActivity())
                            .setTitle(R.string.st_reset_deltas_title)
                            .setMessage(R.string.st_reset_deltas_question)
                            .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                                getActivity().startService(intent);
                                Toast.makeText(getActivity(), R.string.st_reset_deltas_done, Toast.LENGTH_LONG).show();
                            })
                            .setNegativeButton(android.R.string.no, (dialogInterface, i) -> {
                            })
                            .show();
                    return true;
                default:
                    return false;
            }
        }

        /**
         * Enables or disables {@link #mUseRoot} preference depending whether root is available.
         */
        private static class TestRootTask extends AsyncTask<Void, Void, Boolean> {
            private final WeakReference<SettingsFragment> refSettingsFragment;

            TestRootTask(SettingsFragment context) {
                refSettingsFragment = new WeakReference<>(context);
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                return Shell.SU.available();
            }

            @Override
            protected void onPostExecute(Boolean haveRoot) {
                // Get a reference to the fragment if it is still there.
                SettingsFragment settingsFragment = refSettingsFragment.get();
                if (settingsFragment == null) {
                    return;
                }
                if (haveRoot) {
                    settingsFragment.mPendingConfig = true;
                    settingsFragment.mUseRoot.setChecked(true);
                } else {
                    Toast.makeText(settingsFragment.getActivity(), R.string.toast_root_denied, Toast.LENGTH_SHORT)
                            .show();
                }
            }
        }

        /**
         * Handles a new user input for the SOCKS proxy preference.
         * Returns if the changed setting requires a restart.
         */
        private boolean handleSocksProxyPreferenceChange(Preference preference, String newValue) {
            // Valid input is either a proxy address or an empty field to disable the proxy.
            if (newValue.equals("")) {
                preference.setSummary(getString(R.string.do_not_use_proxy) + " " + getString(R.string.generic_example) + ": " + getString(R.string.socks_proxy_address_example));
                return true;
            } else if (newValue.matches("^socks5://.*:\\d{1,5}$")) {
                preference.setSummary(getString(R.string.use_proxy) + " " + newValue);
                return true;
            } else {
                Toast.makeText(getActivity(), R.string.toast_invalid_socks_proxy_address, Toast.LENGTH_SHORT)
                        .show();
                return false;
            }
        }

        /**
         * Handles a new user input for the HTTP(S) proxy preference.
         * Returns if the changed setting requires a restart.
         */
        private boolean handleHttpProxyPreferenceChange(Preference preference, String newValue) {
            // Valid input is either a proxy address or an empty field to disable the proxy.
            if (newValue.equals("")) {
                preference.setSummary(getString(R.string.do_not_use_proxy) + " " + getString(R.string.generic_example) + ": " + getString(R.string.http_proxy_address_example));
                return true;
            } else if (newValue.matches("^http://.*:\\d{1,5}$")) {
                preference.setSummary(getString(R.string.use_proxy) + " " + newValue);
                return true;
            } else {
                Toast.makeText(getActivity(), R.string.toast_invalid_http_proxy_address, Toast.LENGTH_SHORT)
                        .show();
                return false;
            }
        }
    }
}
