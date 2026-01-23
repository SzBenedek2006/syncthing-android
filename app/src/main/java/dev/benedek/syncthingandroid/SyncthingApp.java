package dev.benedek.syncthingandroid;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.StrictMode;
import android.system.Os;
import android.util.Log;

import com.google.android.material.color.DynamicColors;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import dev.benedek.syncthingandroid.util.Languages;

import javax.inject.Inject;

public class SyncthingApp extends Application {

    @Inject DaggerComponent mComponent;

    @Override
    public void onCreate() {
        //DynamicColors.applyToActivitiesIfAvailable(this);

        super.onCreate();
        setupLegacySsl(this);

        DaggerDaggerComponent.builder()
                .syncthingModule(new SyncthingModule(this))
                .build()
                .inject(this);

        new Languages(this).setLanguage(this);

        // The main point here is to use a VM policy without
        // `detectFileUriExposure`, as that leads to exceptions when e.g.
        // opening the ignores file. And it's enabled by default.
        // We might want to disable `detectAll` and `penaltyLog` on release (non-RC) builds too.
        StrictMode.VmPolicy policy = new StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build();
        StrictMode.setVmPolicy(policy);
    }

    public DaggerComponent component() {
        return mComponent;
    }

    private void setupLegacySsl(Context context) {
        // We only need this hack for Android 7.0 (API 24) and below.
        // Android 7.1 (API 25) supports these certs natively.
        if (Build.VERSION.SDK_INT <= 24) {
            try {
                String certName = "isrgrootx1.pem";
                // Result: /data/user/0/dev.benedek.syncthingandroid.debug/files/isrgrootx1.pem
                File certFile = new File(context.getFilesDir(), certName);

                // Copy from assets to internal storage if not already there
                if (!certFile.exists()) {
                    Log.d(this.toString(), "File(context.getFilesDir(), certName) didn't find the file");
                    try (InputStream in = context.getAssets().open(certName);
                         // Result: /data/user/0/dev.benedek.syncthingandroid.debug/files/isrgrootx1.pem
                         FileOutputStream out = new FileOutputStream(certFile)) {
                        byte[] buffer = new byte[1024];
                        int read;
                        while ((read = in.read(buffer)) != -1) {
                            out.write(buffer, 0, read);
                        }
                    }
                }

                Os.setenv("SSL_CERT_FILE", certFile.getAbsolutePath(), true);
                Log.i("SyncthingApp", "Legacy SSL Hack applied using: " + certFile.getAbsolutePath());

            } catch (Exception e) {
                Log.e("SyncthingApp", "Failed to apply Legacy SSL Hack", e);
            }
        }
    }
}
