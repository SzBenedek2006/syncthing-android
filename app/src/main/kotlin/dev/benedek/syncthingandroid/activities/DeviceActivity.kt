package dev.benedek.syncthingandroid.activities

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.CompoundButton
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.gson.Gson
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.databinding.ActivityDeviceBinding
import dev.benedek.syncthingandroid.model.Connections
import dev.benedek.syncthingandroid.model.Device
import dev.benedek.syncthingandroid.service.SyncthingService
import dev.benedek.syncthingandroid.util.Compression
import dev.benedek.syncthingandroid.util.TextWatcherAdapter
import dev.benedek.syncthingandroid.util.Util

/**
 * Shows device details and allows changing them.
 */
class DeviceActivity : SyncthingActivity(), View.OnClickListener {
    private var device: Device? = null

    private var binding: ActivityDeviceBinding? = null

    private var isCreateMode = false

    private var deviceNeedsToUpdate = false

    private var deleteDialog: Dialog? = null
    private var discardDialog: Dialog? = null
    private var compressionDialog: Dialog? = null

    private val serviceStateChangeListener =
        SyncthingService.OnServiceStateChangeListener { currentState -> this@DeviceActivity.onServiceStateChange(currentState) }

    private val compressionEntrySelectedListener: DialogInterface.OnClickListener =
        DialogInterface.OnClickListener { dialog, which ->
            dialog.dismiss()
            val compression = Compression.fromIndex(which)
            // Don't pop the restart dialog unless the value is actually different.
            if (compression != Compression.fromValue(
                    this@DeviceActivity,
                    device!!.compression
                )
            ) {
                deviceNeedsToUpdate = true

                device!!.compression = compression.getValue(this@DeviceActivity)
                binding!!.compressionValue.text = compression.getTitle(this@DeviceActivity)
            }
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

    private val checkedListener: CompoundButton.OnCheckedChangeListener =
        CompoundButton.OnCheckedChangeListener { view, isChecked ->
            when (view.id) {
                R.id.introducer -> {
                    device!!.introducer = isChecked
                    deviceNeedsToUpdate = true
                }

                R.id.devicePause -> {
                    device!!.paused = isChecked
                    deviceNeedsToUpdate = true
                }
            }
        }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceBinding.inflate(layoutInflater)
        setContentView(binding!!.getRoot())


        // Targeting android 15 enables and 16 forces edge-to-edge,
        ViewCompat.setOnApplyWindowInsetsListener(
            binding!!.getRoot()
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
            binding!!.getRoot()
        ) { v: View?, insets: WindowInsetsCompat? ->
            val bars = insets!!.getInsets(
                WindowInsetsCompat.Type.systemBars()
                        or WindowInsetsCompat.Type.displayCutout()
            )
            v!!.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }

        isCreateMode = intent.getBooleanExtra(EXTRA_IS_CREATE, false)
        registerOnServiceConnectedListener { this.onServiceConnected() }
        setTitle(if (isCreateMode) R.string.add_device else R.string.edit_device)

        binding!!.qrButton.setOnClickListener(this)
        binding!!.compressionContainer.setOnClickListener(this)

        if (savedInstanceState != null) {
            if (device == null) {
                device = Gson().fromJson(
                    savedInstanceState.getString("device"),
                    Device::class.java
                )
            }
            restoreDialogStates(savedInstanceState)
        }
        if (isCreateMode) {
            if (device == null) {
                initDevice()
            }
        } else {
            prepareEditMode()
        }
    }

    private fun restoreDialogStates(savedInstanceState: Bundle) {
        if (savedInstanceState.getBoolean(IS_SHOWING_COMPRESSION_DIALOG)) {
            showCompressionDialog()
        }

        if (savedInstanceState.getBoolean(IS_SHOWING_DELETE_DIALOG)) {
            showDeleteDialog()
        }

        if (isCreateMode) {
            if (savedInstanceState.getBoolean(IS_SHOWING_DISCARD_DIALOG)) {
                showDiscardDialog()
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
            syncthingService.unregisterOnServiceStateChangeListener(serviceStateChangeListener)
        }
        binding!!.id.removeTextChangedListener(idTextWatcher)
        binding!!.name.removeTextChangedListener(nameTextWatcher)
        binding!!.addresses.removeTextChangedListener(addressesTextWatcher)
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
        syncthingService.registerOnServiceStateChangeListener(serviceStateChangeListener)
    }

    /**
     * Sets version and current address of the device.
     *
     *
     * NOTE: This is only called once on startup, should be called more often to properly display
     * version/address changes.
     */
    private fun onReceiveConnections(connections: Connections) {
        val viewsExist = binding?.syncthingVersion != null
        if (viewsExist && connections.connectionsMap?.containsKey(device!!.deviceID) == true) {
            binding!!.currentAddress.visibility = View.VISIBLE
            binding!!.syncthingVersion.visibility = View.VISIBLE
            binding!!.currentAddress.text = connections.connectionsMap!![device!!.deviceID]!!.address
            binding!!.syncthingVersion.text = connections.connectionsMap!![device!!.deviceID]!!.clientVersion
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

        api?.getConnections { connections: Connections? ->
            this.onReceiveConnections(
                connections!!
            )
        }

        updateViewsAndSetListeners()
    }

    private fun updateViewsAndSetListeners() {
        binding!!.id.removeTextChangedListener(idTextWatcher)
        binding!!.name.removeTextChangedListener(nameTextWatcher)
        binding!!.addresses.removeTextChangedListener(addressesTextWatcher)
        binding!!.introducer.setOnCheckedChangeListener(null)
        binding!!.devicePause.setOnCheckedChangeListener(null)

        // Update views
        binding!!.id.setText(device!!.deviceID)
        binding!!.name.setText(device!!.name)
        binding!!.addresses.setText(displayableAddresses())
        binding!!.compressionValue.text = Compression.fromValue(this, device!!.compression).getTitle(this)
        binding!!.introducer.setChecked(device!!.introducer)
        binding!!.devicePause.setChecked(device!!.paused)

        // Keep state updated
        binding!!.id.addTextChangedListener(idTextWatcher)
        binding!!.name.addTextChangedListener(nameTextWatcher)
        binding!!.addresses.addTextChangedListener(addressesTextWatcher)
        binding!!.introducer.setOnCheckedChangeListener(checkedListener)
        binding!!.devicePause.setOnCheckedChangeListener(checkedListener)
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.create -> {
                if (TextUtils.isEmpty(device!!.deviceID)) {
                    Toast.makeText(this, R.string.device_id_required, Toast.LENGTH_LONG)
                        .show()
                    return true
                }
                api?.addDevice(
                    device!!
                ) { error: String? ->
                    Toast.makeText(
                        this,
                        error,
                        Toast.LENGTH_LONG
                    ).show()
                }
                finish()
                return true
            }

            R.id.share_device_id -> {
                shareDeviceId(this, device!!.deviceID)
                return true
            }

            R.id.remove -> {
                showDeleteDialog()
                return true
            }

            android.R.id.home -> {
                onBackPressed()
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }


    private fun showDeleteDialog() {
        deleteDialog = createDeleteDialog()
        deleteDialog!!.show()
    }

    private fun createDeleteDialog(): Dialog {
        return Util.getAlertDialogBuilder(this)
            .setMessage(R.string.remove_device_confirm)
            .setPositiveButton(
                android.R.string.ok
            ) { _: DialogInterface?, _: Int ->
                api?.removeDevice(device!!.deviceID)
                finish()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    /**
     * Receives value of scanned QR code and sets it as device ID.
     */
    public override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        if (requestCode == QR_SCAN_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                val scanResult = intent?.getStringExtra(QRScannerActivity.QR_RESULT_ARG)
                if (scanResult != null) {
                    device!!.deviceID = scanResult
                    binding!!.id.setText(device!!.deviceID)
                }
            }
        }
    }

    private fun initDevice() {
        device = Device()
        device!!.name = intent.getStringExtra(EXTRA_DEVICE_NAME).toString()
        device!!.deviceID = intent.getStringExtra(EXTRA_DEVICE_ID)
        device!!.addresses = DYNAMIC_ADDRESS
        device!!.compression = Compression.METADATA.getValue(this)
        device!!.introducer = false
        device!!.paused = false
    }

    private fun prepareEditMode() {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

        val dr = ContextCompat.getDrawable(this, R.drawable.ic_content_copy_24dp)
        binding!!.id.setCompoundDrawablesWithIntrinsicBounds(null, null, dr, null)
        binding!!.id.setEnabled(false)
        binding!!.qrButton.setVisibility(View.GONE)

        binding!!.idContainer.setOnClickListener(this)
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

    override fun onClick(v: View) {
        when (v) {
            binding!!.compressionContainer -> {
                showCompressionDialog()
            }
            binding!!.qrButton -> {
                val qrIntent = QRScannerActivity.intent(this)
                startActivityForResult(qrIntent, QR_SCAN_REQUEST_CODE)
            }
            binding!!.idContainer -> {
                Util.copyDeviceId(this, device!!.deviceID)
            }
        }
    }

    private fun showCompressionDialog() {
        compressionDialog = createCompressionDialog()
        compressionDialog!!.show()
    }

    private fun createCompressionDialog(): Dialog {
        return Util.getAlertDialogBuilder(this)
            .setTitle(R.string.compression)
            .setSingleChoiceItems(
                R.array.compress_entries,
                Compression.fromValue(this, device!!.compression).index,
                compressionEntrySelectedListener
            )
            .create()
    }

    /**
     * Shares the given device ID via Intent. Must be called from an Activity.
     */
    private fun shareDeviceId(context: Context, id: String?) {
        val shareIntent = Intent()
        shareIntent.setAction(Intent.ACTION_SEND)
        shareIntent.setType("text/plain")
        shareIntent.putExtra(Intent.EXTRA_TEXT, id)
        context.startActivity(
            Intent.createChooser(
                shareIntent, context.getString(R.string.send_device_id_to)
            )
        )
    }

    // FIXME
    override fun onBackPressed() {
        if (isCreateMode) {
            showDiscardDialog()
        } else {
            super.onBackPressed()
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
            "activities.syncthingandroid.nutomic.dev.DeviceActivity.NOTIFICATION_ID"
        const val EXTRA_DEVICE_ID: String =
            "activities.syncthingandroid.nutomic.dev.DeviceActivity.DEVICE_ID"
        const val EXTRA_DEVICE_NAME: String =
            "activities.syncthingandroid.nutomic.dev.DeviceActivity.DEVICE_NAME"
        const val EXTRA_IS_CREATE: String =
            "activities.syncthingandroid.nutomic.dev.DeviceActivity.IS_CREATE"

        private const val TAG = "DeviceSettingsFragment"
        private const val IS_SHOWING_DISCARD_DIALOG = "DISCARD_FOLDER_DIALOG_STATE"
        private const val IS_SHOWING_COMPRESSION_DIALOG = "COMPRESSION_FOLDER_DIALOG_STATE"
        private const val IS_SHOWING_DELETE_DIALOG = "DELETE_FOLDER_DIALOG_STATE"
        private const val QR_SCAN_REQUEST_CODE = 777

        private val DYNAMIC_ADDRESS = mutableListOf<String?>("dynamic")
    }
}