package dev.benedek.syncthingandroid.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;

import dev.benedek.syncthingandroid.databinding.ActivityQrScannerBinding;

import java.util.List;

public class QRScannerActivity extends ThemedAppCompatActivity implements BarcodeCallback {

    // region === Static ===
    static final String QR_RESULT_ARG = "QR_CODE";
    static Intent intent(Context context) {
        return new Intent(context, QRScannerActivity.class);
    }
    // endregion

    private final int RC_HANDLE_CAMERA_PERM = 888;

    private ActivityQrScannerBinding binding;

    // region === Activity Lifecycle ===
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQrScannerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Targeting android 15 enables and 16 forces edge-to-edge,
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            mlp.leftMargin = insets.left;
            mlp.bottomMargin = insets.bottom;
            mlp.rightMargin = insets.right;
            v.setLayoutParams(mlp);

            return WindowInsetsCompat.CONSUMED;
        });

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
            );
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        binding.cancelButton.setOnClickListener(view -> {
            finishScanning();
        });

        checkPermissionAndStartScanner();
    }

    @Override
    protected void onStop() {
        super.onStop();
        finishScanning();
    }
    // endregion

    // region === Permissions Callback ===
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RC_HANDLE_CAMERA_PERM) {
            if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanner();
            } else {
                finish();
            }
        }
    }
    // endregion

    // region === BarcodeCallback ===
    @Override
    public void barcodeResult(BarcodeResult result) {
        String code = result.getText();
        Intent intent = new Intent();
        intent.putExtra(QR_RESULT_ARG, code);
        setResult(Activity.RESULT_OK, intent);
        finishScanning();
    }

    @Override
    public void possibleResultPoints(List<ResultPoint> resultPoints) {
        // Unused
    }
    // endregion

    // region === Private Methods ===
    private void checkPermissionAndStartScanner() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = {Manifest.permission.CAMERA};
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
        } else {
            startScanner();
        }
    }

    private void startScanner() {
        binding.barCodeScannerView.resume();
        binding.barCodeScannerView.decodeSingle(this);
    }

    private void finishScanning() {
        binding.barCodeScannerView.pause();
        finish();
    }
    // endregion
}
