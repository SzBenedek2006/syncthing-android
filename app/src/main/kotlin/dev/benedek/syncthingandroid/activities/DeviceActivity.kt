package dev.benedek.syncthingandroid.activities

import android.app.Dialog
import android.content.DialogInterface
import android.content.res.Configuration
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.graphics.toColorInt
import com.google.gson.Gson
import dev.benedek.syncthingandroid.BuildConfig
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.databinding.ActivityDeviceBinding
import dev.benedek.syncthingandroid.model.Device
import dev.benedek.syncthingandroid.model.DeviceStatuses
import dev.benedek.syncthingandroid.service.SyncthingService
import dev.benedek.syncthingandroid.ui.theme.SyncthingandroidTheme
import dev.benedek.syncthingandroid.util.Compression
import dev.benedek.syncthingandroid.util.TextWatcherAdapter
import dev.benedek.syncthingandroid.util.ThemeControls
import dev.benedek.syncthingandroid.util.Util
import dev.benedek.syncthingandroid.viewmodel.DeviceViewModel

/**
 * Shows device details and allows changing them.
 */
class DeviceActivity : SyncthingActivity() {

	private val viewModel: DeviceViewModel by viewModels()
	private var device: Device? = null

	private var binding: ActivityDeviceBinding? = null

	private var isCreateMode = false

	private var deviceNeedsToUpdate = false
		set(value) {
			field = value
			onBackPressedCallback.isEnabled = value
		}

	private var deleteDialog: Dialog? = null
	private var discardDialog: Dialog? = null
	private var compressionDialog: Dialog? = null

	fun serviceStateChangeListener(currentState: SyncthingService.State?) {
		this.onServiceStateChange(currentState)
	}


	private val idTextWatcher: TextWatcher = object : TextWatcherAdapter() {
		override fun afterTextChanged(s: Editable?) {
			if (s.toString() != device!!.deviceID) {
				deviceNeedsToUpdate = true
				device!!.deviceID = s.toString()
			}
		}
	}

	private val nameTextWatcher: TextWatcher = object : TextWatcherAdapter() {
		override fun afterTextChanged(s: Editable?) {
			if (s.toString() != device!!.name) {
				deviceNeedsToUpdate = true
				device!!.name = s.toString()
			}
		}
	}

	private val addressesTextWatcher: TextWatcher = object : TextWatcherAdapter() {
		override fun afterTextChanged(s: Editable?) {
			if (s.toString() != displayableAddresses()) {
				deviceNeedsToUpdate = true
				device!!.addresses = persistableAddresses(s)
			}
		}
	}


	public override fun onCreate(savedInstanceState: Bundle?) {
		isCreateMode = intent.getBooleanExtra(EXTRA_IS_CREATE, false)
		registerOnServiceConnectedListener { this.onServiceConnected() }

		// Needed for pureBlack
		val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
		if (ThemeControls.useDarkMode != null) {
			if (ThemeControls.useDarkMode!! && ThemeControls.pureBlack)
				setTheme(R.style.Theme_Syncthing_Black)
		} else if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
			if (ThemeControls.pureBlack)
				setTheme(R.style.Theme_Syncthing_Black)
		}


		super.onCreate(savedInstanceState)
		onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
		enableEdgeToEdge(
			navigationBarStyle = if (
				ThemeControls.useDarkMode == true ||
				(ThemeControls.useDarkMode == null && currentNightMode == Configuration.UI_MODE_NIGHT_YES)
			) {
				SystemBarStyle.dark("#00000000".toColorInt())
			} else {
				SystemBarStyle.light(
					"#00000000".toColorInt(),
					"#801b1b1b".toColorInt()
				)
			}
		)

		setContent {
			SyncthingandroidTheme(dynamicColor = ThemeControls.isMonetEnabled) {
				dev.benedek.syncthingandroid.ui.DeviceScreen(
					viewModel,
					this::finish
				)
			}
		}
		if (isCreateMode) {
			if (device == null) {
				initDevice()
			}
		}
	}

	public override fun onDestroy() {
		super.onDestroy()
		val syncthingService = service
		if (syncthingService != null) {
			syncthingService.notificationHandler.cancelConsentNotification(
				intent.getIntExtra(
					EXTRA_NOTIFICATION_ID, 0
				)
			)
			syncthingService.unregisterOnServiceStateChangeListener(::serviceStateChangeListener)
		}
		binding?.id?.removeTextChangedListener(idTextWatcher)
		binding?.name?.removeTextChangedListener(nameTextWatcher)
		binding?.addresses?.removeTextChangedListener(addressesTextWatcher)
	}

	public override fun onPause() {
		super.onPause()

		// We don't want to update every time a TextView's character changes,
		// so we hold off until the view stops being visible to the user.
		if (deviceNeedsToUpdate) {
			updateDevice()
		}
	}

	/**
	 * Save current settings in case we are in create mode, and they aren't yet stored in the config.
	 */
	public override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putString("device", Gson().toJson(device))
		if (isCreateMode) {
			outState.putBoolean(
				IS_SHOWING_DISCARD_DIALOG,
				discardDialog != null && discardDialog!!.isShowing
			)
			Util.dismissDialogSafe(discardDialog, this)
		}

		outState.putBoolean(
			IS_SHOWING_COMPRESSION_DIALOG,
			compressionDialog != null && compressionDialog!!.isShowing
		)
		Util.dismissDialogSafe(compressionDialog, this)

		outState.putBoolean(
			IS_SHOWING_DELETE_DIALOG,
			deleteDialog != null && deleteDialog!!.isShowing
		)
		Util.dismissDialogSafe(deleteDialog, this)
	}

	private fun onServiceConnected() {
		Log.v(TAG, "onServiceConnected")
		val syncthingService = service as SyncthingService
		syncthingService.notificationHandler.cancelConsentNotification(
			intent.getIntExtra(
				EXTRA_NOTIFICATION_ID, 0
			)
		)
		syncthingService.registerOnServiceStateChangeListener(::serviceStateChangeListener)

		viewModel.setService(syncthingService)

		viewModel.setInitialState(
			this,
			this::finish,
			isCreate = isCreateMode,
			deviceId = intent.getStringExtra(EXTRA_DEVICE_ID),
			name = intent.getStringExtra(EXTRA_DEVICE_NAME) ?: "",
			addresses = DYNAMIC_ADDRESS,
			compression = Compression.METADATA.getValue(this),
			introducer = false,
			paused = false
		)
	}

	/**
	 * Sets version and current address of the device.
	 *
	 * TODO: This is only called once on startup, should be called more often to properly display version/address changes.
	 */
	private fun onReceiveConnections(deviceStatuses: DeviceStatuses) {
		val map = deviceStatuses.connectionsMap ?: return
		val device = device ?: return

		val deviceExists = map.containsKey(device.deviceID)
		val deviceStatus = map[device.deviceID] ?: return

		if (deviceExists) {
			viewModel.currentAddress = deviceStatus.address
			viewModel.deviceVersion = deviceStatus.clientVersion
		}
	}

	private fun onServiceStateChange(currentState: SyncthingService.State?) {
		if (currentState != SyncthingService.State.ACTIVE) {
			finish()
			return
		}

		if (!isCreateMode) {
			val devices = api?.getDevices(false) ?: emptyList()
			device = null
			for (device in devices) {
				if (device.deviceID == intent.getStringExtra(EXTRA_DEVICE_ID)) {
					this@DeviceActivity.device = device
					break
				}
			}
			if (device == null) {
				Log.w(TAG, "Device not found in API update, maybe it was deleted?")
				finish()
				return
			}
		}

		api?.getConnections { deviceStatuses: DeviceStatuses? ->
			this.onReceiveConnections(
				deviceStatuses!!
			)
		}

	}


	override fun onCreateOptionsMenu(menu: Menu?): Boolean {
		menuInflater.inflate(R.menu.device_settings, menu)
		return super.onCreateOptionsMenu(menu)
	}

	override fun onPrepareOptionsMenu(menu: Menu): Boolean {
		menu.findItem(R.id.create).isVisible = isCreateMode
		menu.findItem(R.id.share_device_id).isVisible = !isCreateMode
		menu.findItem(R.id.remove).isVisible = !isCreateMode
		return true
	}


	private fun initDevice() {
		device = Device()
		device!!.name = intent.getStringExtra(EXTRA_DEVICE_NAME) ?: ""
		device!!.deviceID = intent.getStringExtra(EXTRA_DEVICE_ID)
		device!!.addresses = DYNAMIC_ADDRESS
		device!!.compression = Compression.METADATA.getValue(this)
		device!!.introducer = false
		device!!.paused = false
	}


	/**
	 * Sends the updated device info if in edit mode.
	 */
	private fun updateDevice() {
		if (!isCreateMode && deviceNeedsToUpdate && device != null) {
			api?.editDevice(device!!)
		}
	}

	private fun persistableAddresses(userInput: CharSequence?): MutableList<String?>? {
		return (if (TextUtils.isEmpty(userInput))
			DYNAMIC_ADDRESS
		else
			listOf<String?>(
				*userInput.toString().split(" ".toRegex()).dropLastWhile { it.isEmpty() }
					.toTypedArray())) as MutableList<String?>?
	}

	private fun displayableAddresses(): String? {
		val list = if (DYNAMIC_ADDRESS == device!!.addresses)
			DYNAMIC_ADDRESS
		else
			device!!.addresses
		return TextUtils.join(" ", list!!)
	}



	val onBackPressedCallback = object : OnBackPressedCallback(false) {
		override fun handleOnBackPressed() {
			showDiscardDialog()
		}
	}

	private fun showDiscardDialog() {
		discardDialog = createDiscardDialog()
		discardDialog!!.show()
	}

	private fun createDiscardDialog(): Dialog {
		return Util.getAlertDialogBuilder(this)
			.setMessage(R.string.dialog_discard_changes)
			.setPositiveButton(
				android.R.string.ok
			) { _: DialogInterface, _: Int -> finish() }
			.setNegativeButton(android.R.string.cancel, null)
			.create()
	}

	companion object {
		const val EXTRA_NOTIFICATION_ID: String =
			"${BuildConfig.APPLICATION_ID}.activities.NOTIFICATION_ID"
		const val EXTRA_DEVICE_ID: String =
			"${BuildConfig.APPLICATION_ID}.activities.DEVICE_ID"
		const val EXTRA_DEVICE_NAME: String =
			"${BuildConfig.APPLICATION_ID}.activities.DEVICE_NAME"
		const val EXTRA_IS_CREATE: String =
			"${BuildConfig.APPLICATION_ID}.activities.IS_CREATE"

		private const val TAG = "DeviceSettingsFragment"
		private const val IS_SHOWING_DISCARD_DIALOG = "DISCARD_FOLDER_DIALOG_STATE"
		private const val IS_SHOWING_COMPRESSION_DIALOG = "COMPRESSION_FOLDER_DIALOG_STATE"
		private const val IS_SHOWING_DELETE_DIALOG = "DELETE_FOLDER_DIALOG_STATE"

		private val DYNAMIC_ADDRESS = mutableListOf<String?>("dynamic")
	}
}