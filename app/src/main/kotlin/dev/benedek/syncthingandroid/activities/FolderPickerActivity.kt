package dev.benedek.syncthingandroid.activities

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.SyncthingApp
import dev.benedek.syncthingandroid.service.Constants
import dev.benedek.syncthingandroid.service.SyncthingService
import dev.benedek.syncthingandroid.service.SyncthingServiceBinder
import dev.benedek.syncthingandroid.util.Util
import java.io.File
import java.util.Collections

/**
 * Activity that allows selecting a directory in the local file system.
 */
class FolderPickerActivity : SyncthingActivity(), AdapterView.OnItemClickListener,
    SyncthingService.OnServiceStateChangeListener {
    private var mListView: ListView? = null
    private var mFilesAdapter: FileAdapter? = null
    private var mRootsAdapter: RootsAdapter? = null

    /**
     * Location of null means that the list of roots is displayed.
     */
    private var mLocation: File? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as SyncthingApp).component().inject(this)

        setContentView(R.layout.activity_folder_picker)




        /* Don't set window insets handling here */
        mListView = findViewById(android.R.id.list)
        mListView!!.onItemClickListener = this
        mListView!!.setEmptyView(findViewById(android.R.id.empty))
        mFilesAdapter = FileAdapter(this)
        mRootsAdapter = RootsAdapter(this)
        mListView!!.setAdapter(mFilesAdapter)

        /**
         * Goes up a directory, up to the list of roots if there are multiple roots.
         *
         *
         * If we already are in the list of roots, or if we are directly in the only
         * root folder, we cancel.
         */
        val backCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!mRootsAdapter!!.contains(mLocation) && mLocation != null) {
                    displayFolder(mLocation!!.getParentFile())
                } else if (mRootsAdapter!!.contains(mLocation) && mRootsAdapter!!.count > 1) {
                    displayRoot()
                } else {
                    setResult(RESULT_CANCELED)
                    finish()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, backCallback)
        populateRoots()

        if (intent.hasExtra(EXTRA_INITIAL_DIRECTORY)) {
            displayFolder(File(intent.getStringExtra(EXTRA_INITIAL_DIRECTORY)!!))
        } else {
            displayRoot()
        }

        val prefUseRoot = mPreferences!!.getBoolean(Constants.PREF_USE_ROOT, false)
        if (!prefUseRoot) {
            Toast.makeText(this, R.string.kitkat_external_storage_warning, Toast.LENGTH_LONG)
                .show()
        }
    }

    /**
     * If a root directory is specified it is added to [.mRootsAdapter] otherwise
     * all available storage devices/folders from various APIs are inserted into
     * [.mRootsAdapter].
     */
    @SuppressLint("NewApi")
    private fun populateRoots() {
        val roots = ArrayList<File?>()
        roots.addAll(listOf<File?>(*getExternalFilesDirs(null)))
        roots.remove(getExternalFilesDir(null))

        val rootDir = intent.getStringExtra(EXTRA_ROOT_DIRECTORY)
        if (intent.hasExtra(EXTRA_ROOT_DIRECTORY) && !TextUtils.isEmpty(rootDir)) {
            roots.add(File(rootDir!!))
        } else {
            roots.add(Environment.getExternalStorageDirectory())
            roots.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC))
            roots.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES))
            roots.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
            roots.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM))
            roots.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS))

            // Add paths that might not be accessible to Syncthing.
            if (mPreferences!!.getBoolean("advanced_folder_picker", false)) {
                Collections.addAll(roots, *File("/storage/").listFiles())
                roots.add(File("/"))
            }
        }
        // Remove any invalid directories.
        val it = roots.iterator()
        while (it.hasNext()) {
            val f = it.next()
            if (f == null || !f.exists() || !f.isDirectory()) {
                it.remove()
            }
        }

        mRootsAdapter!!.addAll(roots.filterNotNull().toSortedSet())
    }

    override fun onServiceConnected(componentName: ComponentName?, iBinder: IBinder?) {
        super.onServiceConnected(componentName, iBinder)
        val syncthingServiceBinder = iBinder as SyncthingServiceBinder
        syncthingServiceBinder.service.registerOnServiceStateChangeListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        val syncthingService = service
        syncthingService?.unregisterOnServiceStateChangeListener(this)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (mListView!!.adapter === mRootsAdapter) return true

        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.folder_picker, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.create_folder -> {
                val et = EditText(this)
                val dialog = Util.getAlertDialogBuilder(this)
                    .setTitle(R.string.create_folder)
                    .setView(et)
                    .setPositiveButton(
                        android.R.string.ok
                    ) { _: DialogInterface?, _: Int ->
                        createFolder(
                            et.getText().toString()
                        )
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .create()
                dialog.setOnShowListener {
                    (getSystemService(
                        INPUT_METHOD_SERVICE
                    ) as InputMethodManager)
                        .showSoftInput(et, InputMethodManager.SHOW_IMPLICIT)
                }
                dialog.show()
                return true
            }
            R.id.select -> {
                val intent = Intent()
                    .putExtra(EXTRA_RESULT_DIRECTORY, Util.formatPath(mLocation!!.absolutePath))
                setResult(RESULT_OK, intent)
                finish()
                return true
            }
            android.R.id.home -> {
                finish()
                return true
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
    }


    /**
     * Creates a new folder with the given name and enters it.
     */
    private fun createFolder(name: String) {
        val newFolder = File(mLocation, name)
        if (newFolder.mkdir()) {
            displayFolder(newFolder)
        } else {
            Toast.makeText(this, R.string.create_folder_failed, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Refreshes the ListView to show the contents of the folder in ``mLocation.peek()}.
     */
    private fun displayFolder(folder: File?) {
        mLocation = folder
        mFilesAdapter!!.clear()
        var contents = mLocation!!.listFiles()
        // In case we don't have read access to the folder, just display nothing.
        if (contents == null) contents = arrayOf<File?>()

        contents.sortWith(
            compareByDescending<File> { it.isDirectory }
                .thenBy { it.name }
        )

        for (f in contents) {
            mFilesAdapter!!.add(f)
        }
        mListView!!.setAdapter(mFilesAdapter)
    }

    override fun onItemClick(adapterView: AdapterView<*>?, view: View?, i: Int, l: Long) {
        val item = adapterView?.getItemAtPosition(i)

        if (item is File) {
            if (item.isDirectory) {
                displayFolder(item)
                invalidateOptions()
            }
        }
    }

    private fun invalidateOptions() {
        invalidateOptionsMenu()
    }

    private class FileAdapter(context: Context) :
        ArrayAdapter<File?>(context, R.layout.item_folder_picker) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var convertView = convertView
            convertView = super.getView(position, convertView, parent)
            val title = convertView.findViewById<TextView>(android.R.id.text1)
            val f = getItem(position)
            title.text = f!!.getName()
            val textColor = if (f.isDirectory())
                R.color.md_theme_onPrimary
            else
                R.color.md_theme_onTertiary
            title.setTextColor(ContextCompat.getColor(context, textColor))

            return convertView
        }
    }

    private class RootsAdapter(context: Context) :
        ArrayAdapter<File?>(context, android.R.layout.simple_list_item_1) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var convertView = convertView
            convertView = super.getView(position, convertView, parent)
            val title = convertView.findViewById<TextView>(android.R.id.text1)
            title.text = getItem(position)!!.absolutePath
            return convertView
        }

        fun contains(file: File?): Boolean {
            for (i in 0..<count) {
                if (getItem(i) == file) return true
            }
            return false
        }
    }




    override fun onServiceStateChange(currentState: SyncthingService.State?) {
        if (!isFinishing && currentState != SyncthingService.State.ACTIVE) {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    /**
     * Displays a list of all available roots, or if there is only one root, the
     * contents of that folder.
     */
    private fun displayRoot() {
        mFilesAdapter!!.clear()
        if (mRootsAdapter!!.count == 1) {
            displayFolder(mRootsAdapter!!.getItem(0))
        } else {
            mListView!!.setAdapter(mRootsAdapter)
            mLocation = null
        }
        invalidateOptions()
    }

    companion object {
        private const val EXTRA_INITIAL_DIRECTORY =
            "activities.syncthingandroid.nutomic.dev.FolderPickerActivity.INITIAL_DIRECTORY"

        private const val EXTRA_ROOT_DIRECTORY =
            "activities.syncthingandroid.nutomic.dev.FolderPickerActivity.ROOT_DIRECTORY"

        const val EXTRA_RESULT_DIRECTORY: String =
            "activities.syncthingandroid.nutomic.dev.FolderPickerActivity.RESULT_DIRECTORY"

        const val DIRECTORY_REQUEST_CODE: Int = 234

        @JvmStatic
        fun createIntent(
            context: Context?,
            initialDirectory: String?,
            rootDirectory: String?
        ): Intent {
            val intent = Intent(context, FolderPickerActivity::class.java)

            if (!TextUtils.isEmpty(initialDirectory)) {
                intent.putExtra(EXTRA_INITIAL_DIRECTORY, initialDirectory)
            }

            if (!TextUtils.isEmpty(rootDirectory)) {
                intent.putExtra(EXTRA_ROOT_DIRECTORY, rootDirectory)
            }

            return intent
        }
    }
}