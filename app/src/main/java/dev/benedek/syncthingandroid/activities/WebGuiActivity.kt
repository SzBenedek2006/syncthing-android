package dev.benedek.syncthingandroid.activities

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Proxy
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Parcelable
import android.util.ArrayMap
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.databinding.ActivityWebGuiBinding
import dev.benedek.syncthingandroid.service.Constants
import dev.benedek.syncthingandroid.service.SyncthingService
import dev.benedek.syncthingandroid.service.SyncthingService.OnServiceStateChangeListener
import dev.benedek.syncthingandroid.service.SyncthingServiceBinder
import dev.benedek.syncthingandroid.util.ConfigXml
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.lang.reflect.Constructor
import java.nio.charset.StandardCharsets
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

/**
 * Holds a WebView that shows the web ui of the local syncthing instance.
 */
class WebGuiActivity : StateDialogActivity(), OnServiceStateChangeListener {
    private var mCaCert: X509Certificate? = null

    private var mConfig: ConfigXml? = null

    private var binding: ActivityWebGuiBinding? = null



    /**
     * Initialize WebView.
     *
     * Ignore lint javascript warning as js is loaded only from our known, local service.
     */
    @SuppressLint("SetJavaScriptEnabled")
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebGuiBinding.inflate(layoutInflater)
        setContentView(binding!!.getRoot())

        val backCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                binding?.webview?.goBack()
            }
        }
        onBackPressedDispatcher.addCallback(this, backCallback)
        binding?.webview?.webViewClient = object : WebViewClient() {
            @SuppressLint("WebViewClientOnReceivedSslError")
            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler, error: SslError) {
                if (error.url.startsWith("https://127.0.0.1") || error.url.startsWith("https://localhost")) {
                    handler.proceed()
                } else {
                    handler.cancel()
                }
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest): Boolean {
                if (request.url.host == service.webGuiUrl.host) {
                    return false
                } else {
                    startActivity(Intent(Intent.ACTION_VIEW, request.url))
                    return true
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                binding!!.webview.visibility = View.VISIBLE
                binding!!.loading.visibility = View.GONE
            }
        }

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
        mConfig = ConfigXml(this)
        loadCaCert()

        binding!!.webview.getSettings().javaScriptEnabled = true
        binding!!.webview.getSettings().domStorageEnabled = true
        binding!!.webview.clearCache(true)

        // SyncthingService needs to be started from this activity as the user
        // can directly launch this activity from the recent activity switcher.
        val serviceIntent = Intent(this, SyncthingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    override fun onServiceConnected(componentName: ComponentName?, iBinder: IBinder?) {
        super.onServiceConnected(componentName, iBinder)
        val syncthingServiceBinder = iBinder as SyncthingServiceBinder
        syncthingServiceBinder.service.registerOnServiceStateChangeListener(this)
    }

    override fun onServiceStateChange(newState: SyncthingService.State?) {
        Log.v(TAG, "onServiceStateChange($newState)")
        if (newState == SyncthingService.State.ACTIVE) {
            if (binding!!.webview.getUrl() == null) {
                binding!!.webview.stopLoading()
                setWebViewProxy(
                    binding!!.webview.context.applicationContext,
                    "",
                    0,
                    "localhost|0.0.0.0|127.*|[::1]"
                )
                val credentials = mConfig!!.userName + ":" + mConfig!!.apiKey
                val b64Credentials = Base64.encodeToString(
                    credentials.toByteArray(StandardCharsets.UTF_8),
                    Base64.NO_WRAP
                )
                val headers: MutableMap<String?, String?> = HashMap()
                headers["Authorization"] = "Basic $b64Credentials"
                binding!!.webview.loadUrl(service.webGuiUrl.toString(), headers)
            }
        }
    }

    public override fun onPause() {
        binding!!.webview.onPause()
        binding!!.webview.pauseTimers()
        super.onPause()
    }

    public override fun onResume() {
        super.onResume()
        binding!!.webview.resumeTimers()
        binding!!.webview.onResume()
    }

    override fun onDestroy() {
        val mSyncthingService = service
        mSyncthingService?.unregisterOnServiceStateChangeListener(this)
        binding!!.webview.destroy()
        super.onDestroy()
    }

    /**
     * Reads the SyncthingService.HTTPS_CERT_FILE Ca Cert key and loads it in memory
     */
    private fun loadCaCert() {
        var inStream: InputStream? = null
        val httpsCertFile = Constants.getHttpsCertFile(this)
        if (!httpsCertFile.exists()) {
            Toast.makeText(this@WebGuiActivity, R.string.config_file_missing, Toast.LENGTH_LONG)
                .show()
            finish()
            return
        }
        try {
            inStream = FileInputStream(httpsCertFile)
            val cf = CertificateFactory.getInstance("X.509")
            mCaCert = cf.generateCertificate(inStream) as X509Certificate
        } catch (e: FileNotFoundException) {
            throw IllegalArgumentException("Untrusted Certificate", e)
        } catch (e: CertificateException) {
            throw IllegalArgumentException("Untrusted Certificate", e)
        } finally {
            try {
                inStream?.close()
            } catch (e: IOException) {
                Log.w(TAG, e)
            }
        }
    }

    companion object {
        private const val TAG = "WebGuiActivity"

        /**
         * Set webview proxy and sites that are not retrieved using proxy.
         * Compatible with KitKat or higher android version.
         * Returns boolean if successful.
         * Source: https://stackoverflow.com/a/26781539
         */
        @SuppressLint("PrivateApi", "DiscouragedPrivateApi")
        fun setWebViewProxy(
            appContext: Context?,
            host: String?,
            port: Int,
            exclusionList: String?
        ): Boolean {
            val properties = System.getProperties()
            properties.setProperty("http.proxyHost", host)
            properties.setProperty("http.proxyPort", port.toString())
            properties.setProperty("https.proxyHost", host)
            properties.setProperty("https.proxyPort", port.toString())
            properties.setProperty("http.nonProxyHosts", exclusionList)
            properties.setProperty("https.nonProxyHosts", exclusionList)

            try {
                val applictionCls = Class.forName("android.app.Application")
                val loadedApkField = applictionCls.getDeclaredField("mLoadedApk")
                loadedApkField.isAccessible = true
                val loadedApk = loadedApkField.get(appContext)
                val loadedApkCls = Class.forName("android.app.LoadedApk")
                val receiversField = loadedApkCls.getDeclaredField("mReceivers")
                receiversField.isAccessible = true
                val receivers = receiversField.get(loadedApk) as ArrayMap<*, *>?
                for (receiverMap in receivers!!.values) {
                    for (rec in (receiverMap as ArrayMap<*, *>).keys) {
                        val clazz: Class<*> = rec.javaClass
                        if (clazz.getName().contains("ProxyChangeListener")) {
                            val onReceiveMethod = clazz.getDeclaredMethod(
                                "onReceive",
                                Context::class.java,
                                Intent::class.java
                            )
                            val intent = Intent(Proxy.PROXY_CHANGE_ACTION)
                            val CLASS_NAME = "android.net.ProxyInfo"
                            val cls = Class.forName(CLASS_NAME)
                            val constructor: Constructor<*> = cls.getConstructor(
                                String::class.java,
                                Integer.TYPE,
                                String::class.java
                            )
                            constructor.isAccessible = true
                            val proxyProperties: Any =
                                constructor.newInstance(host, port, exclusionList)
                            intent.putExtra("proxy", proxyProperties as Parcelable)

                            onReceiveMethod.invoke(rec, appContext, intent)
                        }
                    }
                }
                return true
            } catch (e: Exception) {
                Log.w(TAG, "setWebViewProxy exception", e)
            }
            return false
        }
    }
}
