package dev.benedek.syncthingandroid.activities

import android.app.ProgressDialog
import android.content.ContentResolver
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.webkit.MimeTypeMap
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.google.common.io.Files
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.SyncthingApp
import dev.benedek.syncthingandroid.activities.FolderPickerActivity.Companion.createIntent
import dev.benedek.syncthingandroid.activities.SyncthingActivity.OnServiceConnectedListener
import dev.benedek.syncthingandroid.databinding.ActivityShareBinding
import dev.benedek.syncthingandroid.model.Folder
import dev.benedek.syncthingandroid.service.SyncthingService
import dev.benedek.syncthingandroid.service.SyncthingService.OnServiceStateChangeListener
import dev.benedek.syncthingandroid.util.Util
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.lang.ref.WeakReference
import java.text.DateFormat
import java.util.Date

/**
 * Shares incoming files to syncthing folders.
 * 
 * 
 * [.getDisplayNameForUri] and [.getDisplayNameFromContentResolver] are taken from
 * ownCloud Android {@see https://github.com/owncloud/android/blob/79664304fdb762b2e04f1ac505f50d0923ddd212/src/com/owncloud/android/utils/UriUtils.java#L193}
 */
class ShareActivity : StateDialogActivity(), OnServiceConnectedListener,
    OnServiceStateChangeListener {

    private val preferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }
    private var subDirectoryTextView: TextView? = null

    private var foldersSpinner: Spinner? = null

    private var binding: ActivityShareBinding? = null


    override fun onServiceStateChange(currentState: SyncthingService.State?) {
        if (currentState != SyncthingService.State.ACTIVE || api == null) return

        val folders = api!!.folders

        // Get the index of the previously selected folder.
        var folderIndex = 0
        val savedFolderId: String = preferences.getString(
            PREF_PREVIOUSLY_SELECTED_SYNCTHING_FOLDER, ""
        )!!
        for (folder in folders!!) {
            if (folder?.id == savedFolderId) {
                folderIndex = folders.indexOf(folder)
                break
            }
        }

        val adapter: ArrayAdapter<Folder?> = ArrayAdapter<Folder?>(
            this, android.R.layout.simple_spinner_item, folders
        )

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding!!.folders.setAdapter(adapter)
        binding!!.folders.setSelection(folderIndex)
    }

    override fun onServiceConnected() {
        service?.registerOnServiceStateChangeListener(this)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        supportActionBar!!.setDisplayHomeAsUpEnabled(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as SyncthingApp).component().inject(this)
        binding = ActivityShareBinding.inflate(layoutInflater)
        setContentView(binding!!.getRoot())

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

        registerOnServiceConnectedListener(this)

        subDirectoryTextView = findViewById(R.id.sub_directory_Textview)
        foldersSpinner = findViewById(R.id.folders)

        // TODO: add support for EXTRA_TEXT (notes, memos sharing)
        var extrasToCopy: ArrayList<Uri>? = ArrayList()
        if (Intent.ACTION_SEND == intent.action) {
            val uri = intent.getParcelableExtra<Uri?>(Intent.EXTRA_STREAM)
            if (uri != null) extrasToCopy!!.add(uri)
        } else if (Intent.ACTION_SEND_MULTIPLE == intent.action) {
            val extras = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
            if (extras != null) extrasToCopy = extras
        }

        if (extrasToCopy!!.isEmpty()) {
            Toast.makeText(this, getString(R.string.nothing_share), Toast.LENGTH_SHORT).show()
            finish()
        }

        val files: MutableMap<Uri, String> = HashMap()
        for (sourceUri in extrasToCopy) {
            var displayName = getDisplayNameForUri(sourceUri)
            if (displayName == null) {
                displayName = generateDisplayName()
            }
            files[sourceUri] = displayName
        }

        binding!!.name.setText(TextUtils.join("\n", files.values))
        if (files.size > 1) {
            binding!!.name.setFocusable(false)
            binding!!.name.setKeyListener(null)
        }
        binding!!.namesTitle.text = getResources().getQuantityString(
            R.plurals.file_name_title,
            if (files.size > 1) 2 else 1
        )
        binding!!.shareButton.setOnClickListener { _: View? ->
            if (files.size == 1) files.entries.iterator().next()
                .setValue(binding!!.name.getText().toString())
            val folder = foldersSpinner!!.getSelectedItem() as Folder
            val directory = File(folder.path, savedSubDirectory)
            val mCopyFilesTask = CopyFilesTask(this, files, folder, directory)
            mCopyFilesTask.execute()
        }

        foldersSpinner!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                subDirectoryTextView?.text = savedSubDirectory
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        binding!!.browseButton.setOnClickListener { _: View? ->
            val folder = foldersSpinner!!.getSelectedItem() as Folder
            val initialDirectory = File(folder.path, savedSubDirectory)
            startActivityForResult(
                createIntent(
                    applicationContext,
                    initialDirectory.absolutePath, folder.path
                ),
                FolderPickerActivity.DIRECTORY_REQUEST_CODE
            )
        }

        binding!!.cancelButton.setOnClickListener { _: View? -> finish() }
        subDirectoryTextView!!.text = savedSubDirectory
    }

    /**
     * Generate file name for new file.
     */
    private fun generateDisplayName(): String {
        val date = Date(System.currentTimeMillis())
        val df = DateFormat.getDateTimeInstance()
        return String.format(
            getResources().getString(R.string.file_name_template),
            df.format(date)
        )
    }

    /**
     * Get file name from uri.
     */
    private fun getDisplayNameForUri(uri: Uri): String? {
        var displayName: String?

        if (ContentResolver.SCHEME_CONTENT != uri.scheme) {
            displayName = uri.lastPathSegment
        } else {
            displayName = getDisplayNameFromContentResolver(uri)
            if (displayName == null) {
                // last chance to have a name
                displayName = uri.lastPathSegment!!.replace("\\s".toRegex(), "")
            }

            // Add the best possible extension
            val index = displayName.lastIndexOf(".")
            if (index == -1 || MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(displayName.substring(index + 1)) == null
            ) {
                val mimeType = this.contentResolver.getType(uri)
                val extension = MimeTypeMap.getSingleton()
                    .getExtensionFromMimeType(mimeType)
                if (extension != null) {
                    displayName += ".$extension"
                }
            }
        }

        // Replace path separator characters to avoid inconsistent paths
        return displayName?.replace("/".toRegex(), "-")
    }

    /**
     * Get file name from content uri (content://).
     */
    private fun getDisplayNameFromContentResolver(uri: Uri): String? {
        var displayName: String? = null
        val mimeType = contentResolver.getType(uri)
        if (mimeType != null) {
            val displayNameColumn = if (mimeType.startsWith("image/")) {
                MediaStore.Images.ImageColumns.DISPLAY_NAME
            } else if (mimeType.startsWith("video/")) {
                MediaStore.Video.VideoColumns.DISPLAY_NAME
            } else if (mimeType.startsWith("audio/")) {
                MediaStore.Audio.AudioColumns.DISPLAY_NAME
            } else {
                MediaStore.Files.FileColumns.DISPLAY_NAME
            }

            val cursor = contentResolver.query(
                uri,
                arrayOf(displayNameColumn),
                null,
                null,
                null
            )
            if (cursor != null) {
                cursor.moveToFirst()
                displayName = cursor.getString(cursor.getColumnIndexOrThrow(displayNameColumn))
            }
            cursor?.close()
        }
        return displayName
    }

    private val savedSubDirectory: String
        /**
         * Get the previously selected subdirectory for the currently selected Syncthing folder.
         */
        get() {
            val selectedFolder =
                foldersSpinner!!.getSelectedItem() as Folder?
            var savedSubDirectory = ""

            if (selectedFolder != null) {
                savedSubDirectory = preferences.getString(
                    PREF_FOLDER_SAVED_SUBDIRECTORY + selectedFolder.id,
                    ""
                )!!
            }

            return savedSubDirectory
        }

    private class CopyFilesTask(
        context: ShareActivity?,
        private val files: MutableMap<Uri, String>,
        private val folder: Folder,
        private val directory: File?
    ) : AsyncTask<Void?, Void?, Boolean?>() {
        private val refShareActivity: WeakReference<ShareActivity> = WeakReference<ShareActivity>(context)
        private var progress: ProgressDialog? = null
        private var copied = 0
        private var ignored = 0

        override fun onPreExecute() {
            // Get a reference to the activity if it is still there.
            val shareActivity: ShareActivity = refShareActivity.get()!!
            // shareActivity cannot be null before the task executes.
            progress = ProgressDialog.show(
                shareActivity, null,
                shareActivity.getString(R.string.copy_progress), true
            )
        }

        override fun doInBackground(vararg params: Void?): Boolean {
            // Get a reference to the activity if it is still there.
            val shareActivity = refShareActivity.get()
            if (shareActivity == null || shareActivity.isFinishing) {
                cancel(true)
                return true
            }

            var isError = false
            for (entry in files.entries) {
                var inputStream: InputStream? = null
                try {
                    val outFile = File(directory, entry.value)
                    if (outFile.isFile()) {
                        ignored++
                        continue
                    }
                    inputStream = shareActivity.contentResolver.openInputStream(entry.key)
                    if (inputStream != null)
                        Files.asByteSink(outFile).writeFrom(inputStream)
                    copied++
                } catch (e: FileNotFoundException) {
                    Log.e(
                        TAG, String.format(
                            "Can't find input file \"%s\" to copy",
                            entry.key
                        ), e
                    )
                    isError = true
                } catch (e: IOException) {
                    Log.e(
                        TAG, String.format(
                            "IO exception during file \"%s\" sharing",
                            entry.key
                        ), e
                    )
                    isError = true
                } finally {
                    try {
                        inputStream?.close()
                    } catch (e: IOException) {
                        Log.w(TAG, "Exception on input/output stream close", e)
                    }
                }
            }
            return isError
        }

        override fun onPostExecute(isError: Boolean?) {
            // Get a reference to the activity if it is still there.
            val shareActivity = refShareActivity.get()
            if (shareActivity == null || shareActivity.isFinishing) {
                return
            }
            Util.dismissDialogSafe(progress, shareActivity)
            Toast.makeText(
                shareActivity, if (ignored > 0) shareActivity.getResources().getQuantityString(
                    R.plurals.copy_success_partially, copied,
                    copied, folder.label, ignored
                ) else shareActivity.getResources().getQuantityString(
                    R.plurals.copy_success, copied, copied,
                    folder.label
                ),
                Toast.LENGTH_LONG
            ).show()
            if (isError == true) {
                Toast.makeText(
                    shareActivity, shareActivity.getString(R.string.copy_exception),
                    Toast.LENGTH_SHORT
                ).show()
            }
            shareActivity.finish()
        }
    }

    override fun onPause() {
        super.onPause()
        if (foldersSpinner!!.getSelectedItem() != null) {
            val selectedFolder = foldersSpinner!!.getSelectedItem() as Folder
            preferences.edit {
                putString(PREF_PREVIOUSLY_SELECTED_SYNCTHING_FOLDER, selectedFolder.id)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FolderPickerActivity.DIRECTORY_REQUEST_CODE && resultCode == RESULT_OK) {
            val selectedFolder = foldersSpinner!!.getSelectedItem() as Folder
            val folderDirectory: String = Util.formatPath(selectedFolder.path!!)!!
            var subDirectory = data?.getStringExtra(FolderPickerActivity.EXTRA_RESULT_DIRECTORY)
            //Remove the parent directory from the string, so it is only the Sub directory that is displayed to the user.
            subDirectory = subDirectory!!.replace(folderDirectory, "")
            subDirectoryTextView!!.text = subDirectory

            preferences.edit {
                putString(PREF_FOLDER_SAVED_SUBDIRECTORY + selectedFolder.id, subDirectory)
            }
        }
    }

    companion object {
        private const val TAG = "ShareActivity"
        private const val PREF_PREVIOUSLY_SELECTED_SYNCTHING_FOLDER =
            "previously_selected_syncthing_folder"

        const val PREF_FOLDER_SAVED_SUBDIRECTORY: String = "saved_sub_directory_"
    }
}
