package dev.benedek.syncthingandroid.views

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.material.color.MaterialColors
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.databinding.ItemFolderListBinding
import dev.benedek.syncthingandroid.model.Folder
import dev.benedek.syncthingandroid.model.FolderStatus
import dev.benedek.syncthingandroid.service.Constants
import dev.benedek.syncthingandroid.service.RestApi
import dev.benedek.syncthingandroid.service.RestApi.OnResultListener2
import dev.benedek.syncthingandroid.service.SyncthingService
import dev.benedek.syncthingandroid.util.Util
import java.io.File
import kotlin.math.roundToInt
import androidx.core.net.toUri

/**
 * Generates item views for folder items.
 */
class FoldersAdapter(private val context: Context) : ArrayAdapter<Folder?>(
    context, 0
) {
    private val localFolderStatuses = HashMap<String?, FolderStatus?>()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding = if (convertView == null)
            DataBindingUtil.inflate(
                LayoutInflater.from(context),
                R.layout.item_folder_list,
                parent,
                false
            )
        else
            DataBindingUtil.bind<ItemFolderListBinding?>(convertView)

        val folder = getItem(position)
        binding!!.label.text = if (TextUtils.isEmpty(folder!!.label)) folder.id else folder.label
        binding.directory.text = folder.path
        binding.override.setOnClickListener { _: View? ->
            // Send "Override changes" through our service to the REST API.
            val intent = Intent(context, SyncthingService::class.java)
                .putExtra(SyncthingService.EXTRA_FOLDER_ID, folder.id)
            intent.setAction(SyncthingService.ACTION_OVERRIDE_CHANGES)
            context.startService(intent)
        }
        binding.openFolder.setOnClickListener(View.OnClickListener { _: View? ->
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.fromFile(File(folder.path!!)), "resource/folder")
            intent.putExtra("org.openintents.extra.ABSOLUTE_PATH", folder.path)
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                // Try a second way to find a compatible file explorer app.
                Log.v(TAG, "openFolder: Fallback to application chooser to open folder.")
                intent.setDataAndType(folder.path!!.toUri(), "application/*")
                val chooserIntent =
                    Intent.createChooser(intent, context.getString(R.string.open_file_manager))
                if (chooserIntent != null) {
                    context.startActivity(chooserIntent)
                } else {
                    Toast.makeText(context, R.string.toast_no_file_manager, Toast.LENGTH_SHORT)
                        .show()
                }
            }
        })

        updateFolderStatusView(binding, folder)
        return binding.root
    }

    private fun updateFolderStatusView(binding: ItemFolderListBinding, folder: Folder) {
        val folderStatus = localFolderStatuses[folder.id]
        if (folderStatus == null) {
            binding.items.visibility = View.GONE
            binding.override.visibility = View.GONE
            binding.size.visibility = View.GONE
            setTextOrHide(binding.invalid, folder.invalid)
            return
        }

        val neededItems =
            folderStatus.needFiles + folderStatus.needDirectories + folderStatus.needSymlinks + folderStatus.needDeletes
        val outOfSync = folderStatus.state == "idle" && neededItems > 0
        val overrideButtonVisible = folder.type == Constants.FOLDER_TYPE_SEND_ONLY && outOfSync
        binding.override.visibility = if (overrideButtonVisible) View.VISIBLE else View.GONE
        if (outOfSync) {
            binding.state.text = context.getString(R.string.status_outofsync)
            binding.state.setTextColor(ContextCompat.getColor(context, R.color.text_red))
        } else {
            if (folder.paused) {
                binding.state.text = context.getString(R.string.state_paused)
                binding.state.setTextColor(
                    MaterialColors.getColor(
                        context,
                        android.R.attr.textColorPrimary,
                        Color.BLACK
                    )
                )
            } else {
                binding.state.text = getLocalizedState(context, folderStatus)
                when (folderStatus.state) {
                    "idle" -> binding.state.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.text_green
                        )
                    )

                    "scanning", "syncing" -> binding.state.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.md_theme_onPrimaryContainer
                        )
                    )

                    "error" -> binding.state.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.text_red
                        )
                    )

                    else -> binding.state.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.text_red
                        )
                    )
                }
            }
        }
        binding.items.visibility = View.VISIBLE
        binding.items.text = context.resources
            .getQuantityString(
                R.plurals.files,
                folderStatus.inSyncFiles.toInt(),
                folderStatus.inSyncFiles,
                folderStatus.globalFiles
            )
        binding.size.visibility = View.VISIBLE
        binding.size.text = context.getString(
            R.string.folder_size_format,
            Util.readableFileSize(context, folderStatus.inSyncBytes),
            Util.readableFileSize(context, folderStatus.globalBytes)
        )
        setTextOrHide(binding.invalid, folderStatus.invalid)
    }

    /**
     * Requests updated folder status from the api for all visible items.
     */
    fun updateFolderStatus(api: RestApi) {
        for (i in 0..<count) {
            api.getFolderStatus(
                getItem(i)!!.id!!
            ) { folderId: String?, folderStatus: FolderStatus? ->
                this.onReceiveFolderStatus(
                    folderId,
                    folderStatus
                )
            }
        }
    }

    private fun onReceiveFolderStatus(folderId: String?, folderStatus: FolderStatus?) {
        localFolderStatuses[folderId] = folderStatus
        notifyDataSetChanged()
    }

    private fun setTextOrHide(view: TextView, text: String?) {
        if (TextUtils.isEmpty(text)) {
            view.visibility = View.GONE
        } else {
            view.text = text
            view.visibility = View.VISIBLE
        }
    }

    companion object {
        private const val TAG = "FoldersAdapter"

        /**
         * Returns the folder's state as a localized string.
         */
        private fun getLocalizedState(c: Context, folderStatus: FolderStatus): String {
            when (folderStatus.state) {
                "idle" -> return c.getString(R.string.state_idle)
                "scanning" -> return c.getString(R.string.state_scanning)
                "syncing" -> {
                    val percentage = if (folderStatus.globalBytes != 0L)
                        (100f * folderStatus.inSyncBytes / folderStatus.globalBytes).roundToInt()
                    else
                        100
                    return c.getString(R.string.state_syncing, percentage)
                }

                "error" -> {
                    if (TextUtils.isEmpty(folderStatus.error)) {
                        return c.getString(R.string.state_error)
                    }
                    return c.getString(R.string.state_error) + " (" + folderStatus.error + ")"
                }

                "unknown" -> return c.getString(R.string.state_unknown)
                else -> return folderStatus.state.toString()
            }
        }
    }
}
