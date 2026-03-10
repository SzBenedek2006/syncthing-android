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
import dev.benedek.syncthingandroid.activities.DeviceActivity
import dev.benedek.syncthingandroid.activities.SyncthingActivity
import dev.benedek.syncthingandroid.model.Device
import dev.benedek.syncthingandroid.service.Constants
import dev.benedek.syncthingandroid.service.SyncthingService
import dev.benedek.syncthingandroid.service.SyncthingService.OnServiceStateChangeListener
import dev.benedek.syncthingandroid.views.DevicesAdapter
import java.util.Collections
import java.util.Timer
import java.util.TimerTask

/**
 * Displays a list of all existing devices.
 */
class DeviceListFragment : ListFragment(), OnServiceStateChangeListener, OnItemClickListener {
    private var adapter: DevicesAdapter? = null

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

                activity!!.runOnUiThread { this@DeviceListFragment.updateList() }
            }
        }, 0, Constants.GUI_UPDATE_INTERVAL)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)
        setEmptyText(getString(R.string.devices_list_empty))
        getListView().onItemClickListener = this
    }

    /**
     * Refreshes ListView by updating devices and info.
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
        val devices = restApi.getDevices(false) ?: return
        if (adapter == null) {
            adapter = DevicesAdapter(activity)
            setListAdapter(adapter)
        }

        // Prevent scroll position reset due to list update from clear().
        adapter!!.setNotifyOnChange(false)
        adapter!!.clear()
        Collections.sort(devices, DEVICES_COMPARATOR)
        adapter!!.addAll(devices)
        adapter!!.updateConnections(restApi)
        adapter!!.notifyDataSetChanged()
        setListShown(true)
    }

    override fun onItemClick(adapterView: AdapterView<*>?, view: View?, i: Int, l: Long) {
        val intent = Intent(activity, DeviceActivity::class.java)
        intent.putExtra(DeviceActivity.EXTRA_IS_CREATE, false)
        intent.putExtra(DeviceActivity.EXTRA_DEVICE_ID, adapter!!.getItem(i)!!.deviceID)
        startActivity(intent)
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.device_list, menu)
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.add_device) {
            val intent = Intent(activity, DeviceActivity::class.java)
                .putExtra(DeviceActivity.EXTRA_IS_CREATE, true)
            startActivity(intent)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private val DEVICES_COMPARATOR =
            Comparator { lhs: Device?, rhs: Device? -> lhs!!.name.compareTo(rhs!!.name) }
    }
}
