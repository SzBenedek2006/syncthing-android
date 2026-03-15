package dev.benedek.syncthingandroid.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import dev.benedek.syncthingandroid.databinding.ActivityQrScannerBinding

class QRScannerActivity : ThemedAppCompatActivity(), BarcodeCallback {
    // endregion
    private val RC_HANDLE_CAMERA_PERM = 888

    private var binding: ActivityQrScannerBinding? = null

    // region === Activity Lifecycle ===
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrScannerBinding.inflate(layoutInflater)
        setContentView(binding!!.getRoot())

        // Targeting android 15 enables and 16 forces edge-to-edge,
        ViewCompat.setOnApplyWindowInsetsListener(
            binding!!.getRoot()
        ) { v: View?, windowInsets: WindowInsetsCompat? ->
            val insets = windowInsets!!.getInsets(WindowInsetsCompat.Type.systemBars())
            val mlp = v!!.layoutParams as MarginLayoutParams
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

        binding!!.cancelButton.setOnClickListener { _: View? ->
            finishScanning()
        }

        checkPermissionAndStartScanner()
    }

    override fun onStop() {
        super.onStop()
        finishScanning()
    }

    // endregion
    // region === Permissions Callback ===
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RC_HANDLE_CAMERA_PERM) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanner()
            } else {
                finish()
            }
        }
    }

    // endregion
    // region === BarcodeCallback ===
    override fun barcodeResult(result: BarcodeResult) {
        val code = result.text
        val intent = Intent()
        intent.putExtra(QR_RESULT_ARG, code)
        setResult(RESULT_OK, intent)
        finishScanning()
    }

    override fun possibleResultPoints(resultPoints: MutableList<ResultPoint?>?) {
        // Unused
    }

    // endregion
    // region === Private Methods ===
    private fun checkPermissionAndStartScanner() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            val permissions = arrayOf<String?>(Manifest.permission.CAMERA)
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM)
        } else {
            startScanner()
        }
    }

    private fun startScanner() {
        binding!!.barCodeScannerView.resume()
        binding!!.barCodeScannerView.decodeSingle(this)
    }

    private fun finishScanning() {
        binding!!.barCodeScannerView.pause()
        finish()
    } // endregion

    companion object {
        // region === Static ===
        const val QR_RESULT_ARG: String = "QR_CODE"
        fun intent(context: Context?): Intent {
            return Intent(context, QRScannerActivity::class.java)
        }
    }
}
