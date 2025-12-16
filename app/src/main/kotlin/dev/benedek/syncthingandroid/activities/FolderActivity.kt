package dev.benedek.syncthingandroid.activities

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.MarginLayoutParamsCompat
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.documentfile.provider.DocumentFile
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.gson.Gson
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.databinding.FragmentFolderBinding
import dev.benedek.syncthingandroid.model.Device
import dev.benedek.syncthingandroid.model.Folder
import dev.benedek.syncthingandroid.service.Constants
import dev.benedek.syncthingandroid.service.SyncthingService
import dev.benedek.syncthingandroid.util.FileUtils
import dev.benedek.syncthingandroid.util.TextWatcherAdapter
import dev.benedek.syncthingandroid.util.Util
import java.io.File
import java.io.IOException
import java.util.Random
import java.util.concurrent.TimeUnit

/**
 * Shows folder details and allows changing them.
 */
class FolderActivity : SyncthingActivity(), SyncthingActivity.OnServiceConnectedListener,
    SyncthingService.OnServiceStateChangeListener {
    private var mFolder: Folder? = null
    private var mFolderUri: Uri? = null

    // Indicates the result of the write test to mFolder.path on dialog init or after a path change.
    var mCanWriteToPath: Boolean = false

    private var binding: FragmentFolderBinding? = null

    private var mIsCreateMode = false
    private var mFolderNeedsToUpdate = false

    private var mDeleteDialog: Dialog? = null
    private var mDiscardDialog: Dialog? = null

    private var mVersioning: Folder.Versioning? = null

    private val mTextWatcher: TextWatcher = object : TextWatcherAdapter() {
        override fun afterTextChanged(s: Editable?) {
            mFolder!!.label = binding!!.label.getText().toString()
            mFolder!!.id = binding!!.id.getText().toString()
            // binding.directoryTextView must not be handled here as it's handled by {@link onActivityResult}
            mFolderNeedsToUpdate = true
        }
    }

    private val mCheckedListener: CompoundButton.OnCheckedChangeListener =
        object : CompoundButton.OnCheckedChangeListener {
            override fun onCheckedChanged(view: CompoundButton, isChecked: Boolean) {
                when (view.getId()) {
                    R.id.fileWatcher -> {
                        mFolder!!.fsWatcherEnabled = isChecked
                        mFolderNeedsToUpdate = true
                    }

                    R.id.folderPause -> {
                        mFolder!!.paused = isChecked
                        mFolderNeedsToUpdate = true
                    }

                    R.id.device_toggle -> {
                        val device = view.getTag() as Device
                        if (isChecked) {
                            mFolder!!.addDevice(device.deviceID)
                        } else {
                            mFolder!!.removeDevice(device.deviceID)
                        }
                        mFolderNeedsToUpdate = true
                    }
                }
            }
        }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentFolderBinding.inflate(getLayoutInflater())
        setContentView(binding!!.getRoot())

        // Targeting android 15 enables and 16 forces edge-to-edge,
        ViewCompat.setOnApplyWindowInsetsListener(
            binding!!.getRoot(),
            OnApplyWindowInsetsListener { v: View?, windowInsets: WindowInsetsCompat? ->
                val insets = windowInsets!!.getInsets(WindowInsetsCompat.Type.systemBars())
                val mlp = v!!.getLayoutParams() as ViewGroup.MarginLayoutParams
                mlp.leftMargin = insets.left
                mlp.bottomMargin = insets.bottom
                mlp.rightMargin = insets.right
                v.setLayoutParams(mlp)
                WindowInsetsCompat.CONSUMED
            })

        ViewCompat.setOnApplyWindowInsetsListener(
            binding!!.getRoot(),
            OnApplyWindowInsetsListener { v: View?, insets: WindowInsetsCompat? ->
                val bars = insets!!.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            or WindowInsetsCompat.Type.displayCutout()
                )
                v!!.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                WindowInsetsCompat.CONSUMED
            })

        mIsCreateMode = getIntent().getBooleanExtra(EXTRA_IS_CREATE, false)
        setTitle(if (mIsCreateMode) R.string.create_folder else R.string.edit_folder)
        registerOnServiceConnectedListener(this)

        binding!!.directoryTextView.setOnClickListener(View.OnClickListener { view: View? -> onPathViewClick() })

        findViewById<View?>(R.id.folderTypeContainer).setOnClickListener(View.OnClickListener { v: View? -> showFolderTypeDialog() })
        findViewById<View?>(R.id.pullOrderContainer).setOnClickListener(View.OnClickListener { v: View? -> showPullOrderDialog() })
        findViewById<View?>(R.id.versioningContainer).setOnClickListener(View.OnClickListener { v: View? -> showVersioningDialog() })
        binding!!.editIgnores.setOnClickListener(View.OnClickListener { v: View? -> editIgnores() })

        if (mIsCreateMode) {
            if (savedInstanceState != null) {
                mFolder = Gson().fromJson<Folder?>(
                    savedInstanceState.getString("folder"),
                    Folder::class.java
                )
                if (savedInstanceState.getBoolean(IS_SHOW_DISCARD_DIALOG)) {
                    showDiscardDialog()
                }
            }
            if (mFolder == null) {
                initFolder()
            }
            // Open keyboard on label view in edit mode.
            binding!!.label.requestFocus()
            binding!!.editIgnores.setEnabled(false)
        } else {
            // Prepare edit mode.
            binding!!.id.clearFocus()
            binding!!.id.setFocusable(false)
            binding!!.id.setEnabled(false)
            binding!!.directoryTextView.setEnabled(false)
        }

        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean(IS_SHOWING_DELETE_DIALOG)) {
                showDeleteDialog()
            }
        }

        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean(IS_SHOWING_DELETE_DIALOG)) {
                showDeleteDialog()
            }
        }
    }

    /**
     * Invoked after user clicked on the directoryTextView label.
     */
    @SuppressLint("InlinedAPI")
    private fun onPathViewClick() {
        // This has to be android.net.Uri as it implements a Parcelable.
        val externalFilesDirUri = FileUtils.getExternalFilesDirUri(this@FolderActivity)

        // Display storage access framework directory picker UI.
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        if (externalFilesDirUri != null) {
            intent.putExtra("android.provider.extra.INITIAL_URI", externalFilesDirUri)
        }
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
        intent.putExtra("android.content.extra.SHOW_ADVANCED", true)
        try {
            startActivityForResult(intent, CHOOSE_FOLDER_REQUEST)
        } catch (e: ActivityNotFoundException) {
            Log.e(
                TAG,
                "onPathViewClick exception, falling back to built-in FolderPickerActivity.",
                e
            )
            startActivityForResult(
                FolderPickerActivity.createIntent(this, mFolder!!.path, null),
                FolderPickerActivity.DIRECTORY_REQUEST_CODE
            )
        }
    }

    private fun editIgnores() {
        try {
            val ignoreFile = File(mFolder!!.path, IGNORE_FILE_NAME)
            if (!ignoreFile.exists() && !ignoreFile.createNewFile()) {
                Toast.makeText(this, R.string.create_ignore_file_error, Toast.LENGTH_SHORT).show()
                return
            }
            val intent = Intent(Intent.ACTION_EDIT)
            val uri = Uri.fromFile(ignoreFile)
            intent.setDataAndType(uri, "text/plain")
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

            startActivity(intent)
        } catch (e: IOException) {
            Log.w(TAG, e)
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, e)
            Toast.makeText(this, R.string.edit_ignore_file_error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showFolderTypeDialog() {
        if (TextUtils.isEmpty(mFolder!!.path)) {
            Toast.makeText(this, R.string.folder_path_required, Toast.LENGTH_LONG)
                .show()
            return
        }
        if (!mCanWriteToPath) {
            /**
             * Do not handle the click as the children in the folder type layout are disabled
             * and an explanation is already given on the UI why the only allowed folder type
             * is "sendonly".
             */
            Toast.makeText(this, R.string.folder_path_readonly, Toast.LENGTH_LONG)
                .show()
            return
        }
        // The user selected folder path is writeable, offer to choose from all available folder types.
        val intent = Intent(this, FolderTypeDialogActivity::class.java)
        intent.putExtra(FolderTypeDialogActivity.EXTRA_FOLDER_TYPE, mFolder!!.type)
        startActivityForResult(intent, FOLDER_TYPE_DIALOG_REQUEST)
    }

    private fun showPullOrderDialog() {
        val intent = Intent(this, PullOrderDialogActivity::class.java)
        intent.putExtra(PullOrderDialogActivity.EXTRA_PULL_ORDER, mFolder!!.order)
        startActivityForResult(intent, PULL_ORDER_DIALOG_REQUEST)
    }

    private fun showVersioningDialog() {
        val intent = Intent(this, VersioningDialogActivity::class.java)
        intent.putExtras(this.versioningBundle)
        startActivityForResult(intent, FILE_VERSIONING_DIALOG_REQUEST)
    }

    private val versioningBundle: Bundle
        get() {
            val bundle = Bundle()
            for (entry in mFolder!!.versioning.params.entries) {
                bundle.putString(entry.key, entry.value)
            }

            if (TextUtils.isEmpty(mFolder!!.versioning.type)) {
                bundle.putString("type", "none")
            } else {
                bundle.putString("type", mFolder!!.versioning.type)
            }

            return bundle
        }

    public override fun onDestroy() {
        super.onDestroy()
        val syncthingService = getService()
        if (syncthingService != null) {
            syncthingService.getNotificationHandler().cancelConsentNotification(
                getIntent().getIntExtra(
                    EXTRA_NOTIFICATION_ID, 0
                )
            )
            syncthingService.unregisterOnServiceStateChangeListener(SyncthingService.OnServiceStateChangeListener { currentState: SyncthingService.State? ->
                this.onServiceStateChange(
                    currentState
                )
            })
        }
        binding!!.label.removeTextChangedListener(mTextWatcher)
        binding!!.id.removeTextChangedListener(mTextWatcher)
    }

    public override fun onPause() {
        super.onPause()

        // We don't want to update every time a TextView's character changes,
        // so we hold off until the view stops being visible to the user.
        if (mFolderNeedsToUpdate) {
            updateFolder()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(
            IS_SHOWING_DELETE_DIALOG,
            mDeleteDialog != null && mDeleteDialog!!.isShowing()
        )
        Util.dismissDialogSafe(mDeleteDialog, this)

        if (mIsCreateMode) {
            outState.putBoolean(
                IS_SHOW_DISCARD_DIALOG,
                mDiscardDialog != null && mDiscardDialog!!.isShowing()
            )
            Util.dismissDialogSafe(mDiscardDialog, this)
        }
    }

    /**
     * Save current settings in case we are in create mode and they aren't yet stored in the config.
     */
    override fun onServiceConnected() {
        Log.v(TAG, "onServiceConnected")
        val syncthingService = getService() as SyncthingService
        syncthingService.getNotificationHandler().cancelConsentNotification(
            getIntent().getIntExtra(
                EXTRA_NOTIFICATION_ID, 0
            )
        )
        syncthingService.registerOnServiceStateChangeListener(this)
    }

    override fun onServiceStateChange(currentState: SyncthingService.State?) {
        if (currentState != SyncthingService.State.ACTIVE) {
            finish()
            return
        }

        if (!mIsCreateMode) {
            val folders = getApi().getFolders()
            val passedId = getIntent().getStringExtra(EXTRA_FOLDER_ID)
            mFolder = null
            for (currentFolder in folders) {
                if (currentFolder.id == passedId) {
                    mFolder = currentFolder
                    break
                }
            }
            if (mFolder == null) {
                Log.w(TAG, "Folder not found in API update, maybe it was deleted?")
                finish()
                return
            }
            checkWriteAndUpdateUI()
        }
        if (getIntent().hasExtra(EXTRA_DEVICE_ID)) {
            mFolder!!.addDevice(getIntent().getStringExtra(EXTRA_DEVICE_ID))
            mFolderNeedsToUpdate = true
        }

        attemptToApplyVersioningConfig()

        updateViewsAndSetListeners()
    }

    // If the FolderActivity gets recreated after the VersioningDialogActivity is closed, then the result from the VersioningDialogActivity will be received before
    // the mFolder variable has been recreated, so the versioning config will be stored in the mVersioning variable until the mFolder variable has been
    // recreated in the onServiceStateChange(). This has been observed to happen after the screen orientation has changed while the VersioningDialogActivity was open.
    private fun attemptToApplyVersioningConfig() {
        if (mFolder != null && mVersioning != null) {
            mFolder!!.versioning = mVersioning
            mVersioning = null
        }
    }

    private fun updateViewsAndSetListeners() {
        binding!!.label.removeTextChangedListener(mTextWatcher)
        binding!!.id.removeTextChangedListener(mTextWatcher)
        binding!!.fileWatcher.setOnCheckedChangeListener(null)
        binding!!.folderPause.setOnCheckedChangeListener(null)

        // Update views
        binding!!.label.setText(mFolder!!.label)
        binding!!.id.setText(mFolder!!.id)
        updateFolderTypeDescription()
        updatePullOrderDescription()
        updateVersioningDescription()
        binding!!.fileWatcher.setChecked(mFolder!!.fsWatcherEnabled)
        binding!!.folderPause.setChecked(mFolder!!.paused)
        val devicesList = getApi().getDevices(false)

        binding!!.devicesContainer.removeAllViews()
        if (devicesList.isEmpty()) {
            addEmptyDeviceListView()
        } else {
            for (n in devicesList) {
                addDeviceViewAndSetListener(n, getLayoutInflater())
            }
        }

        // Keep state updated
        binding!!.label.addTextChangedListener(mTextWatcher)
        binding!!.id.addTextChangedListener(mTextWatcher)
        binding!!.fileWatcher.setOnCheckedChangeListener(mCheckedListener)
        binding!!.folderPause.setOnCheckedChangeListener(mCheckedListener)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        getMenuInflater().inflate(R.menu.folder_settings, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.create).setVisible(mIsCreateMode)
        menu.findItem(R.id.remove).setVisible(!mIsCreateMode)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.getItemId()) {
            R.id.create -> {
                if (TextUtils.isEmpty(mFolder!!.id)) {
                    Toast.makeText(this, R.string.folder_id_required, Toast.LENGTH_LONG)
                        .show()
                    return true
                }
                if (TextUtils.isEmpty(mFolder!!.path)) {
                    Toast.makeText(this, R.string.folder_path_required, Toast.LENGTH_LONG)
                        .show()
                    return true
                }
                if (mFolderUri != null) {
                    /**
                     * Normally, syncthing takes care of creating the ".stfolder" marker.
                     * This fails on newer android versions if the syncthing binary only has
                     * readonly access on the path and the user tries to configure a
                     * sendonly folder. To fix this, we'll precreate the marker using java code.
                     * We also create an empty file in the marker directory, to hopefully keep
                     * it alive in the face of overzealous disk cleaner apps.
                     */
                    val dfFolder = DocumentFile.fromTreeUri(this, mFolderUri!!)
                    if (dfFolder != null) {
                        Log.v(
                            TAG,
                            "Creating new directory " + mFolder!!.path + File.separator + FOLDER_MARKER_NAME
                        )
                        val marker = dfFolder.createDirectory(FOLDER_MARKER_NAME)
                        marker!!.createFile("text/plain", "empty")
                    }
                }
                getApi().createFolder(mFolder)
                finish()
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
        mDeleteDialog = createDeleteDialog()
        mDeleteDialog!!.show()
    }

    private fun createDeleteDialog(): Dialog {
        return Util.getAlertDialogBuilder(this)
            .setMessage(R.string.remove_folder_confirm)
            .setPositiveButton(
                android.R.string.yes,
                DialogInterface.OnClickListener { dialogInterface: DialogInterface?, i: Int ->
                    val restApi = getApi()
                    if (restApi != null) {
                        restApi.removeFolder(mFolder!!.id)
                    }
                    mFolderNeedsToUpdate = false
                    finish()
                })
            .setNegativeButton(android.R.string.no, null)
            .create()
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && requestCode == CHOOSE_FOLDER_REQUEST) {
            mFolderUri = data?.getData()
            if (mFolderUri == null) {
                return
            }
            // Get the folder path unix style, e.g. "/storage/0000-0000/DCIM"
            var targetPath = FileUtils.getAbsolutePathFromSAFUri(this@FolderActivity, mFolderUri)
            if (targetPath != null) {
                targetPath = Util.formatPath(targetPath)
            }
            if (targetPath == null || TextUtils.isEmpty(targetPath) || (targetPath == File.separator)) {
                mFolder!!.path = ""
                mFolderUri = null
                checkWriteAndUpdateUI()
                // Show message to the user suggesting to select a folder on internal or external storage.
                Toast.makeText(this, R.string.toast_invalid_folder_selected, Toast.LENGTH_LONG)
                    .show()
                return
            }
            mFolder!!.path = FileUtils.cutTrailingSlash(targetPath)
            Log.v(
                TAG,
                "onActivityResult/CHOOSE_FOLDER_REQUEST: Got directory path '" + mFolder!!.path + "'"
            )
            checkWriteAndUpdateUI()
            // Postpone sending the config changes using syncthing REST API.
            mFolderNeedsToUpdate = true
        } else if (resultCode == RESULT_OK && requestCode == FolderPickerActivity.DIRECTORY_REQUEST_CODE) {
            mFolder!!.path = data?.getStringExtra(FolderPickerActivity.EXTRA_RESULT_DIRECTORY)
            checkWriteAndUpdateUI()
            // Postpone sending the config changes using syncthing REST API.
            mFolderNeedsToUpdate = true
        } else if (resultCode == RESULT_OK && requestCode == FILE_VERSIONING_DIALOG_REQUEST) {
            updateVersioning(data?.getExtras()!!)
        } else if (resultCode == RESULT_OK && requestCode == FOLDER_TYPE_DIALOG_REQUEST) {
            mFolder!!.type = data?.getStringExtra(FolderTypeDialogActivity.EXTRA_RESULT_FOLDER_TYPE)
            updateFolderTypeDescription()
            mFolderNeedsToUpdate = true
        } else if (resultCode == RESULT_OK && requestCode == PULL_ORDER_DIALOG_REQUEST) {
            mFolder!!.order = data?.getStringExtra(PullOrderDialogActivity.EXTRA_RESULT_PULL_ORDER)
            updatePullOrderDescription()
            mFolderNeedsToUpdate = true
        }
    }

    /**
     * Prerequisite: mFolder.path must be non-empty
     */
    private fun checkWriteAndUpdateUI() {
        binding!!.directoryTextView.setText(mFolder!!.path)
        if (TextUtils.isEmpty(mFolder!!.path)) {
            return
        }

        /**
         * Check if the permissions we have on that folder is readonly or readwrite.
         * Access level readonly: folder can only be configured "sendonly".
         * Access level readwrite: folder can be configured "sendonly" or "sendreceive".
         */
        mCanWriteToPath = Util.nativeBinaryCanWriteToPath(this@FolderActivity, mFolder!!.path)
        if (mCanWriteToPath) {
            binding!!.accessExplanationView.setText(R.string.folder_path_readwrite)
            binding!!.folderType.setEnabled(true)
            binding!!.editIgnores.setEnabled(true)
            if (mIsCreateMode) {
                /**
                 * Suggest folder type FOLDER_TYPE_SEND_RECEIVE for folders to be created
                 * because the user most probably intentionally chose a special folder like
                 * "[storage]/Android/data/dev.benedek.syncthingandroid/files"
                 * or enabled root mode thus having write access.
                 */
                mFolder!!.type = Constants.FOLDER_TYPE_SEND_RECEIVE
                updateFolderTypeDescription()
            }
        } else {
            // Force "sendonly" folder.
            binding!!.accessExplanationView.setText(R.string.folder_path_readonly)
            binding!!.folderType.setEnabled(false)
            binding!!.editIgnores.setEnabled(false)
            mFolder!!.type = Constants.FOLDER_TYPE_SEND_ONLY
            updateFolderTypeDescription()
        }
    }

    private fun generateRandomFolderId(): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray()
        val sb = StringBuilder()
        val random = Random()
        for (i in 0..9) {
            if (i == 5) {
                sb.append("-")
            }
            val c = chars[random.nextInt(chars.size)]
            sb.append(c)
        }
        return sb.toString()
    }

    private fun initFolder() {
        mFolder = Folder()
        mFolder!!.id = if (getIntent().hasExtra(EXTRA_FOLDER_ID))
            getIntent().getStringExtra(EXTRA_FOLDER_ID)
        else
            generateRandomFolderId()
        mFolder!!.label = getIntent().getStringExtra(EXTRA_FOLDER_LABEL)
        mFolder!!.fsWatcherEnabled = true
        mFolder!!.fsWatcherDelayS = 10
        /**
         * Folder rescan interval defaults to 3600s as it is the default in
         * syncthing when the file watcher is enabled and a new folder is created.
         */
        mFolder!!.rescanIntervalS = 3600
        mFolder!!.paused = false
        mFolder!!.type = Constants.FOLDER_TYPE_SEND_RECEIVE
        mFolder!!.versioning = Folder.Versioning()
    }

    private fun addEmptyDeviceListView() {
        val height = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            48f,
            getResources().getDisplayMetrics()
        ).toInt()
        val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, height)
        val dividerInset = getResources().getDimensionPixelOffset(R.dimen.material_divider_inset)
        val contentInset =
            getResources().getDimensionPixelOffset(R.dimen.abc_action_bar_content_inset_material)
        MarginLayoutParamsCompat.setMarginStart(params, dividerInset)
        MarginLayoutParamsCompat.setMarginEnd(params, contentInset)
        val emptyView = TextView(binding!!.devicesContainer.getContext())
        emptyView.setGravity(Gravity.CENTER_VERTICAL)
        emptyView.setText(R.string.devices_list_empty)
        binding!!.devicesContainer.addView(emptyView, params)
    }

    private fun addDeviceViewAndSetListener(device: Device, inflater: LayoutInflater) {
        inflater.inflate(R.layout.item_device_form, binding!!.devicesContainer)
        val deviceView =
            binding!!.devicesContainer.getChildAt(binding!!.devicesContainer.getChildCount() - 1) as MaterialSwitch
        deviceView.setOnCheckedChangeListener(null)
        deviceView.setChecked(mFolder!!.getDevice(device.deviceID) != null)
        deviceView.setText(device.getDisplayName())
        deviceView.setTag(device)
        deviceView.setOnCheckedChangeListener(mCheckedListener)
    }

    private fun updateFolder() {
        if (!mIsCreateMode) {
            /**
             * RestApi is guaranteed not to be null as [onServiceStateChange]
             * immediately finishes this activity if SyncthingService shuts down.
             */
            getApi().updateFolder(mFolder)
        }
    }

    override fun onBackPressed() {
        if (mIsCreateMode) {
            showDiscardDialog()
        } else {
            super.onBackPressed()
        }
    }

    private fun showDiscardDialog() {
        mDiscardDialog = createDiscardDialog()
        mDiscardDialog!!.show()
    }

    private fun createDiscardDialog(): Dialog {
        return Util.getAlertDialogBuilder(this)
            .setMessage(R.string.dialog_discard_changes)
            .setPositiveButton(
                android.R.string.ok,
                DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int -> finish() })
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    private fun updateVersioning(arguments: Bundle) {
        if (mFolder != null) {
            mVersioning = mFolder!!.versioning
        } else {
            mVersioning = Folder.Versioning()
        }

        val type = arguments.getString("type")
        arguments.remove("type")

        if (type == "none") {
            mVersioning = Folder.Versioning()
        } else {
            for (key in arguments.keySet()) {
                mVersioning!!.params.put(key, arguments.getString(key))
            }
            mVersioning!!.type = type
        }

        attemptToApplyVersioningConfig()
        updateVersioningDescription()
        mFolderNeedsToUpdate = true
    }

    private fun updateFolderTypeDescription() {
        if (mFolder == null) {
            return
        }

        when (mFolder!!.type) {
            Constants.FOLDER_TYPE_SEND_RECEIVE -> setFolderTypeDescription(
                getString(R.string.folder_type_sendreceive),
                getString(R.string.folder_type_sendreceive_description)
            )

            Constants.FOLDER_TYPE_SEND_ONLY -> setFolderTypeDescription(
                getString(R.string.folder_type_sendonly),
                getString(R.string.folder_type_sendonly_description)
            )

            Constants.FOLDER_TYPE_RECEIVE_ONLY -> setFolderTypeDescription(
                getString(R.string.folder_type_receiveonly),
                getString(R.string.folder_type_receiveonly_description)
            )
        }
    }

    private fun setFolderTypeDescription(type: String?, description: String?) {
        binding!!.folderType.setText(type)
        binding!!.folderTypeDescription.setText(description)
    }

    private fun updatePullOrderDescription() {
        if (mFolder == null) {
            return
        }

        if (TextUtils.isEmpty(mFolder!!.order)) {
            setPullOrderDescription(
                getString(R.string.pull_order_type_random),
                getString(R.string.pull_order_type_random_description)
            )
            return
        }

        when (mFolder!!.order) {
            "random" -> setPullOrderDescription(
                getString(R.string.pull_order_type_random),
                getString(R.string.pull_order_type_random_description)
            )

            "alphabetic" -> setPullOrderDescription(
                getString(R.string.pull_order_type_alphabetic),
                getString(R.string.pull_order_type_alphabetic_description)
            )

            "smallestFirst" -> setPullOrderDescription(
                getString(R.string.pull_order_type_smallestFirst),
                getString(R.string.pull_order_type_smallestFirst_description)
            )

            "largestFirst" -> setPullOrderDescription(
                getString(R.string.pull_order_type_largestFirst),
                getString(R.string.pull_order_type_largestFirst_description)
            )

            "oldestFirst" -> setPullOrderDescription(
                getString(R.string.pull_order_type_oldestFirst),
                getString(R.string.pull_order_type_oldestFirst_description)
            )

            "newestFirst" -> setPullOrderDescription(
                getString(R.string.pull_order_type_newestFirst),
                getString(R.string.pull_order_type_newestFirst_description)
            )
        }
    }

    private fun setPullOrderDescription(type: String?, description: String?) {
        binding!!.pullOrderType.setText(type)
        binding!!.pullOrderDescription.setText(description)
    }

    private fun updateVersioningDescription() {
        if (mFolder == null) {
            return
        }

        if (TextUtils.isEmpty(mFolder!!.versioning.type)) {
            setVersioningDescription(getString(R.string.none), "")
            return
        }

        when (mFolder!!.versioning.type) {
            "simple" -> setVersioningDescription(
                getString(R.string.type_simple),
                getString(R.string.simple_versioning_info, mFolder!!.versioning.params.get("keep"))
            )

            "trashcan" -> setVersioningDescription(
                getString(R.string.type_trashcan),
                getString(
                    R.string.trashcan_versioning_info,
                    mFolder!!.versioning.params.get("cleanoutDays")
                )
            )

            "staggered" -> {
                val maxAge =
                    TimeUnit.SECONDS.toDays(mFolder!!.versioning.params.get("maxAge")!!.toLong())
                        .toInt()
                setVersioningDescription(
                    getString(R.string.type_staggered),
                    getString(
                        R.string.staggered_versioning_info,
                        maxAge,
                        mFolder!!.versioning.params.get("versionsPath")
                    )
                )
            }

            "external" -> setVersioningDescription(
                getString(R.string.type_external),
                getString(
                    R.string.external_versioning_info,
                    mFolder!!.versioning.params.get("command")
                )
            )
        }
    }

    private fun setVersioningDescription(type: String?, description: String?) {
        binding!!.versioningType.setText(type)
        binding!!.versioningDescription.setText(description)
    }

    companion object {
        const val EXTRA_NOTIFICATION_ID: String =
            "activities.syncthingandroid.nutomic.dev.FolderActivity.NOTIFICATION_ID"
        const val EXTRA_IS_CREATE: String =
            "activities.syncthingandroid.nutomic.dev.FolderActivity.IS_CREATE"
        const val EXTRA_FOLDER_ID: String =
            "activities.syncthingandroid.nutomic.dev.FolderActivity.FOLDER_ID"
        const val EXTRA_FOLDER_LABEL: String =
            "activities.syncthingandroid.nutomic.dev.FolderActivity.FOLDER_LABEL"
        const val EXTRA_DEVICE_ID: String =
            "activities.syncthingandroid.nutomic.dev.FolderActivity.DEVICE_ID"

        private const val TAG = "FolderActivity"

        private const val IS_SHOWING_DELETE_DIALOG = "DELETE_FOLDER_DIALOG_STATE"
        private const val IS_SHOW_DISCARD_DIALOG = "DISCARD_FOLDER_DIALOG_STATE"

        private const val FILE_VERSIONING_DIALOG_REQUEST = 3454
        private const val PULL_ORDER_DIALOG_REQUEST = 3455
        private const val FOLDER_TYPE_DIALOG_REQUEST = 3456
        private const val CHOOSE_FOLDER_REQUEST = 3459

        private const val FOLDER_MARKER_NAME = ".stfolder"
        private const val IGNORE_FILE_NAME = ".stignore"
    }
}