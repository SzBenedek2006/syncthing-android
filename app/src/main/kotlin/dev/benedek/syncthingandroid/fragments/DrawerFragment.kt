@file:Suppress("UnusedVariable")

package dev.benedek.syncthingandroid.fragments

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Bundle
import androidx.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.android.volley.VolleyError
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.activities.MainActivity
import dev.benedek.syncthingandroid.activities.SettingsActivity
import dev.benedek.syncthingandroid.activities.WebGuiActivity
import dev.benedek.syncthingandroid.http.ImageGetRequest
import dev.benedek.syncthingandroid.model.Connections
import dev.benedek.syncthingandroid.model.SystemInfo
import dev.benedek.syncthingandroid.model.SystemVersion
import dev.benedek.syncthingandroid.service.Constants
import dev.benedek.syncthingandroid.service.RestApi
import dev.benedek.syncthingandroid.util.Util
import java.text.NumberFormat
import java.util.Timer
import java.util.TimerTask

/**
 * Displays information about the local device.
 */
@Suppress("unused")
class DrawerFragment : Fragment(), View.OnClickListener {
    private var ramUsage: TextView? = null
    private var download: TextView? = null
    private var upload: TextView? = null
    private var announceServer: TextView? = null
    private var version: TextView? = null
    private var exitButton: TextView? = null

    private var timer: Timer? = null

    private lateinit var mainActivity: MainActivity
    private var sharedPreferences: SharedPreferences? = null

    fun onDrawerOpened() {
        timer = Timer()
        timer!!.schedule(object : TimerTask() {
            override fun run() {
                updateGui()
            }
        }, 0, Constants.GUI_UPDATE_INTERVAL)
    }

    override fun onResume() {
        super.onResume()
    }

    fun onDrawerClosed() {
        if (timer != null) {
            timer!!.cancel()
            timer = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        onDrawerClosed()
    }

    /**
     * Populates views and menu.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_drawer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mainActivity = requireActivity() as MainActivity
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mainActivity)


        ramUsage = view.findViewById(R.id.ram_usage)
        download = view.findViewById(R.id.download)
        upload = view.findViewById(R.id.upload)
        announceServer = view.findViewById(R.id.announce_server)
        version = view.findViewById(R.id.version)
        exitButton = view.findViewById(R.id.drawerActionExit)

        view.findViewById<View>(R.id.drawerActionWebGui)
            .setOnClickListener(this)
        view.findViewById<View>(R.id.drawerActionRestart)
            .setOnClickListener(this)
        view.findViewById<View>(R.id.drawerActionSettings)
            .setOnClickListener(this)
        view.findViewById<View>(R.id.drawerActionShowQrCode)
            .setOnClickListener(this)
        exitButton!!.setOnClickListener(this)

        if (savedInstanceState != null && savedInstanceState.getBoolean("active")) {
            onDrawerOpened()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("active", timer != null)
    }

    /**
     * Invokes status callbacks.
     */
    private fun updateGui() {
        val mainActivity = requireActivity() as MainActivity
        if (mainActivity.isFinishing) {
            return
        }

        val mApi = mainActivity.api
        if (mApi != null) {
            mApi.getSystemInfo { info: SystemInfo? ->
                this.onReceiveSystemInfo(
                    info!!
                )
            }
            mApi.getSystemVersion { info: SystemVersion? ->
                this.onReceiveSystemVersion(
                    info!!
                )
            }
            mApi.getConnections { connections: Connections? ->
                this.onReceiveConnections(
                    connections!!
                )
            }
        }
    }

    /**
     * This will not do anything if gui updates are already scheduled.
     */
    fun requestGuiUpdate() {
        if (timer == null) {
            updateGui()
        }
    }

    /**
     * Populates views with status received via [RestApi.getSystemInfo].
     */
    @SuppressLint("SetTextI18n")
    private fun onReceiveSystemInfo(info: SystemInfo) {
        requireActivity()
        val percentFormat = NumberFormat.getPercentInstance()
        percentFormat.setMaximumFractionDigits(2)
        ramUsage!!.text = Util.readableFileSize(mainActivity, info.sys)
        val announceTotal = info.discoveryMethods
        val announceConnected = announceTotal - (info.discoveryErrors?.size ?: 0)

        announceServer!!.text = "$announceConnected/$announceTotal"
        val color = if (announceConnected > 0)
            R.color.text_green
        else
            R.color.text_red
        announceServer!!.setTextColor(ContextCompat.getColor(requireContext(), color))
    }

    /**
     * Populates views with status received via [RestApi.getSystemInfo].
     */
    private fun onReceiveSystemVersion(info: SystemVersion) {
        requireActivity()
        version!!.text = info.version
    }

    /**
     * Populates views with status received via [RestApi.getConnections].
     */
    private fun onReceiveConnections(connections: Connections) {
        val total = connections.total
        download!!.text = Util.readableTransferRate(mainActivity, total?.inBits ?: 0)
        upload!!.text = Util.readableTransferRate(mainActivity, total?.outBits ?: 0)
    }

    /**
     * IMPLEMENTED IN COMPOSE!
     * Gets QRCode and displays it in a Dialog.
     */
    private fun showQrCode() {
        val restApi = mainActivity.api
        if (restApi == null) {
            Toast.makeText(mainActivity, R.string.syncthing_terminated, Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val apiKey = restApi.gui?.apiKey
            val deviceId = restApi.localDevice?.deviceID
            val url = restApi.url
            //The QRCode request takes one paramteer called "text", which is the text to be converted to a QRCode.
            ImageGetRequest(
                mainActivity,
                url,
                ImageGetRequest.QR_CODE_GENERATOR,
                apiKey!!,
                mutableMapOf("text" to deviceId),
                { qrCodeBitmap: Bitmap? ->
                    mainActivity.showQrCodeDialog(deviceId, qrCodeBitmap)
                    mainActivity.closeDrawer()
                },
                { _: VolleyError? ->
                    Toast.makeText(
                        mainActivity,
                        R.string.could_not_access_deviceid,
                        Toast.LENGTH_SHORT
                    ).show()
                })
        } catch (e: Exception) {
            Log.e(TAG, "showQrCode", e)
        }
    }

    // IMPLEMENTED IN COMPOSE!
    override fun onClick(v: View) {
        when (v.id) {
            R.id.drawerActionWebGui -> {
                startActivity(Intent(mainActivity, WebGuiActivity::class.java))
                mainActivity.closeDrawer()
            }
            R.id.drawerActionSettings -> {
                startActivity(Intent(mainActivity, SettingsActivity::class.java))
                mainActivity.closeDrawer()
            }
            R.id.drawerActionRestart -> {
                mainActivity.showRestartDialog()
                mainActivity.closeDrawer()
            }
            R.id.drawerActionExit -> {
                if (sharedPreferences != null && sharedPreferences!!.getBoolean(
                        Constants.PREF_START_SERVICE_ON_BOOT,
                        false
                    )
                ) {
                    /*
                     * App is running as a service. Show an explanation why exiting syncthing is an
                     * extraordinary request, then ask the user to confirm.
                     */
                    val mExitConfirmationDialog = Util.getAlertDialogBuilder(mainActivity)
                        .setTitle(R.string.dialog_exit_while_running_as_service_title)
                        .setMessage(R.string.dialog_exit_while_running_as_service_message)
                        .setPositiveButton(
                            R.string.yes
                        ) { _: DialogInterface?, _: Int ->
                            mainActivity.doExit()
                        }
                        .setNegativeButton(
                            R.string.no
                        ) { _: DialogInterface?, _: Int -> }
                        .show()
                } else {
                    // App is not running as a service.
                    mainActivity.doExit()
                }
                mainActivity.closeDrawer()
            }
            R.id.drawerActionShowQrCode -> {
                showQrCode()
            }
        }
    }



    companion object {
        private const val TAG = "DrawerFragment"
    }
}
