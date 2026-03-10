package dev.benedek.syncthingandroid.fragments

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import androidx.fragment.app.ListFragment
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.activities.FolderActivity
import dev.benedek.syncthingandroid.activities.SyncthingActivity
import dev.benedek.syncthingandroid.service.Constants
import dev.benedek.syncthingandroid.service.SyncthingService
import dev.benedek.syncthingandroid.service.SyncthingService.OnServiceStateChangeListener
import dev.benedek.syncthingandroid.ui.FolderViewModel
import dev.benedek.syncthingandroid.views.FoldersAdapter
import java.util.Timer
import java.util.TimerTask

/**
 * Displays a list of all existing folders.
 */
class FolderListFragment : ListFragment(), OnServiceStateChangeListener, OnItemClickListener {
    private var adapter: FoldersAdapter? = null

    private var timer: Timer? = null

    override fun onPause() {
        super.onPause()
        if (timer != null) {
            timer!!.cancel()
        }
    }

    override fun onServiceStateChange(currentState: SyncthingService.State?) {
        if (currentState != SyncthingService.State.ACTIVE) return

        timer = Timer()
        timer!!.schedule(object : TimerTask() {
            override fun run() {
                if (activity == null) return

                requireActivity().runOnUiThread { this@FolderListFragment.updateList() }
            }
        }, 0, Constants.GUI_UPDATE_INTERVAL)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)
        setEmptyText(getString(R.string.folder_list_empty))
        getListView().onItemClickListener = this
    }

    /**
     * Refreshes ListView by updating folders and info.
     * 
     * 
     * Also creates adapter if it doesn't exist yet.
     */
    private fun updateList() {
        val activity = activity as SyncthingActivity?
        if (activity == null || view == null || activity.isFinishing) {
            return
        }
        val restApi = activity.api
        if (restApi == null || !restApi.isConfigLoaded) {
            return
        }
        val folders = restApi.folders ?: return
        if (adapter == null) {
            adapter = FoldersAdapter(activity)
            setListAdapter(adapter)
        }

        // Prevent scroll position reset due to list update from clear().
        adapter!!.setNotifyOnChange(false)
        adapter!!.clear()
        adapter!!.addAll(folders)
        adapter!!.updateFolderStatus(restApi)
        adapter!!.notifyDataSetChanged()
        setListShown(true)
    }

    override fun onItemClick(adapterView: AdapterView<*>?, view: View?, i: Int, l: Long) {
        val intent = Intent(activity, FolderActivity::class.java)
            .putExtra(FolderViewModel.EXTRA_IS_CREATE, false)
            .putExtra(FolderViewModel.EXTRA_FOLDER_ID, adapter!!.getItem(i)!!.id)
        startActivity(intent)
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.folder_list, menu)
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.add_folder) {
            val intent = Intent(activity, FolderActivity::class.java)
                .putExtra(FolderViewModel.EXTRA_IS_CREATE, true)
            startActivity(intent)
            return true
        } else {
            return super.onOptionsItemSelected(item)
        }
    }
}


