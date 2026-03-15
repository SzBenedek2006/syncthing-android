package dev.benedek.syncthingandroid.activities

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.databinding.DialogLoadingBinding
import dev.benedek.syncthingandroid.model.RunConditionCheckResult
import dev.benedek.syncthingandroid.model.RunConditionCheckResult.BlockerReason
import dev.benedek.syncthingandroid.service.SyncthingService
import dev.benedek.syncthingandroid.util.Util.dismissDialogSafe
import dev.benedek.syncthingandroid.util.Util.getAlertDialogBuilder
import java.util.concurrent.TimeUnit

/**
 * Handles loading/disabled dialogs.
 */
abstract class StateDialogActivity : SyncthingActivity() {
    private var serviceState = SyncthingService.State.INIT
    private var loadingDialog: AlertDialog? = null
    private var disabledDialog: AlertDialog? = null
    private var isPaused = true

    private val onServiceStateChangeListener =
        SyncthingService.OnServiceStateChangeListener { currentState: SyncthingService.State? ->
            this.onServiceStateChange(
                currentState!!
            )
        }
    private val onRunConditionCheckResultListener =
        SyncthingService.OnRunConditionCheckResultListener { result: RunConditionCheckResult? ->
            this.onRunConditionCheckResultChange(
                result
            )
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerOnServiceConnectedListener {
            service?.registerOnServiceStateChangeListener(onServiceStateChangeListener)
            service?.registerOnRunConditionCheckResultChange(onRunConditionCheckResultListener)
        }
    }

    override fun onResume() {
        super.onResume()
        isPaused = false
        when (serviceState) {
            SyncthingService.State.DISABLED -> showDisabledDialog()
            else -> {}
        }
    }

    override fun onPause() {
        super.onPause()
        isPaused = true
        dismissDisabledDialog()
        dismissLoadingDialog()
    }


    override fun onDestroy() {
        super.onDestroy()
        service?.unregisterOnServiceStateChangeListener(onServiceStateChangeListener)
        service?.unregisterOnRunConditionCheckResultChange(onRunConditionCheckResultListener)
        dismissDisabledDialog()
    }

    private fun onServiceStateChange(currentState: SyncthingService.State) {
        serviceState = currentState
        when (serviceState) {
            SyncthingService.State.INIT, SyncthingService.State.STARTING -> {
                dismissDisabledDialog()
                showLoadingDialog()
            }

            SyncthingService.State.ACTIVE -> {
                dismissDisabledDialog()
                dismissLoadingDialog()
            }

            SyncthingService.State.DISABLED -> if (!isPaused) {
                showDisabledDialog()
            }

            SyncthingService.State.ERROR -> {}
        }
    }

    private fun onRunConditionCheckResultChange(result: RunConditionCheckResult?) { // FIXME
        if (disabledDialog != null && disabledDialog!!.isShowing) {
            disabledDialog!!.setMessage(this.disabledDialogMessage)
        }
    }

    private fun showDisabledDialog() {
        if (this.isFinishing && (disabledDialog != null)) {
            return
        }

        disabledDialog = getAlertDialogBuilder(this)
            .setTitle(R.string.syncthing_disabled_title)
            .setMessage(this.disabledDialogMessage)
            .setPositiveButton(
                R.string.syncthing_disabled_change_settings
            ) { _: DialogInterface?, _: Int ->
                val intent = Intent(this, SettingsActivity::class.java)
                intent.putExtra(
                    SettingsActivity.EXTRA_OPEN_SUB_PREF_SCREEN,
                    "category_run_conditions"
                )
                startActivity(intent)
            }
            .setNegativeButton(
                R.string.exit
            ) { _: DialogInterface?, _: Int ->
                ActivityCompat.finishAffinity(
                    this
                )
            }
            .setCancelable(false)
            .show()
    }

    private val disabledDialogMessage: StringBuilder
        get() {
            val message = java.lang.StringBuilder()
            message.append(this.getResources().getString(R.string.syncthing_disabled_message))
            val reasons: MutableList<BlockerReason?> =
                service?.currentRunConditionCheckResult!!.blockReasons
            if (!reasons.isEmpty()) {
                message.append("\n")
                message.append("\n")
                message.append(
                    this.getResources().getString(R.string.syncthing_disabled_reason_heading)
                )
                var count = 0
                for (reason in reasons) {
                    count++
                    message.append("\n")
                    if (reasons.size > 1) message.append("$count. ")
                    message.append(this.getString(reason?.resId ?: 0))
                }
            }
            return message
        }

    private fun dismissDisabledDialog() {
        dismissDialogSafe(disabledDialog, this)
        disabledDialog = null
    }

    /**
     * Shows the loading dialog with the correct text ("creating keys" or "loading").
     */
    private fun showLoadingDialog() {
        if (isPaused || loadingDialog != null) return

        val binding = DataBindingUtil.inflate<DialogLoadingBinding>(
            layoutInflater, R.layout.dialog_loading, null, false
        )
        val isGeneratingKeys = intent.getBooleanExtra(EXTRA_KEY_GENERATION_IN_PROGRESS, false)
        binding.loadingText.setText(
            if (isGeneratingKeys)
                R.string.web_gui_creating_key
            else
                R.string.api_loading
        )

        loadingDialog = getAlertDialogBuilder(this)
            .setCancelable(false)
            .setView(binding.root)
            .show()

        if (!isGeneratingKeys) {
            Handler().postDelayed({
                if (this.isFinishing || loadingDialog == null) return@postDelayed
                binding.loadingSlowMessage.visibility = View.VISIBLE
                binding.viewLogs.setOnClickListener { _: View? ->
                    startActivity(
                        Intent(this, LogActivity::class.java)
                    )
                }
            }, SLOW_LOADING_TIME)
        }
    }

    private fun dismissLoadingDialog() {
        dismissDialogSafe(loadingDialog, this)
        loadingDialog = null
    }

    companion object {
        private val SLOW_LOADING_TIME = TimeUnit.SECONDS.toMillis(30)
    }
}
