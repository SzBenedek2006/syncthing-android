package dev.benedek.syncthingandroid.util

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.service.Constants
import dev.benedek.syncthingandroid.service.SyncthingRunnable
import dev.benedek.syncthingandroid.util.Util.fixAppDataPermissions
import org.mindrot.jbcrypt.BCrypt
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xml.sax.SAXException
import java.io.File
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.util.Random
import javax.inject.Inject
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * Provides direct access to the config.xml file in the file system.
 *
 * This class should only be used if the syncthing API is not available (usually during startup).
 */
class ConfigXml(private val context: Context) {
    class OpenConfigException : RuntimeException()

    @JvmField
    @Inject
    var preferences: SharedPreferences? = null

    private val configFile: File = Constants.getConfigFile(this.context)

    private var config: Document? = null

    init {
        val isFirstStart = !configFile.exists()
        if (isFirstStart) {
            Log.i(TAG, "App started for the first time. Generating keys and config.")
            SyncthingRunnable(context, SyncthingRunnable.Command.generate).run()
        }

        readConfig()

        if (isFirstStart) {
            var changed = false

            Log.i(TAG, "Starting syncthing to retrieve local device id.")
            val logOutput = SyncthingRunnable(context, SyncthingRunnable.Command.deviceid).run(true)
            val localDeviceID = logOutput.replace("\n", "")
            // Verify local device ID is correctly formatted.
            if (localDeviceID.matches("^([A-Z0-9]{7}-){7}[A-Z0-9]{7}$".toRegex())) {
                changed = changeLocalDeviceName(localDeviceID)
            }
            changed = changeDefaultFolder() || changed

            // Save changes if we made any.
            if (changed) {
                saveChanges()
            }
        }
    }

    private fun readConfig() {
        if (!configFile.canRead() && !fixAppDataPermissions(context)) {
            throw OpenConfigException()
        }
        try {
            val db = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            Log.d(TAG, "Trying to read '$configFile'")
            config = db.parse(configFile)
        } catch (e: SAXException) {
            Log.w(TAG, "Cannot read '$configFile'", e)
            throw OpenConfigException()
        } catch (e: ParserConfigurationException) {
            Log.w(TAG, "Cannot read '$configFile'", e)
            throw OpenConfigException()
        } catch (e: IOException) {
            Log.w(TAG, "Cannot read '$configFile'", e)
            throw OpenConfigException()
        }
        Log.i(TAG, "Loaded Syncthing config file")
    }

    val webGuiUrl: URL
        get() {
            val urlProtocol =
                if (Constants.osSupportsTLS12()) "https" else "http"
            try {
                return URL(
                    "$urlProtocol://" + this.guiElement.getElementsByTagName("address").item(0)
                        .textContent
                )
            } catch (e: MalformedURLException) {
                throw RuntimeException("Failed to parse web interface URL", e)
            }
        }

    val apiKey: String?
        get() = this.guiElement.getElementsByTagName("apikey").item(0).textContent

    val userName: String?
        get() = this.guiElement.getElementsByTagName("user").item(0).textContent

    /**
     * Updates the config file.
     *
     * Sets ignorePerms flag to true on every folder, force enables TLS, sets the
     * username/password, and disables weak hash checking.
     */
    fun updateIfNeeded() {
        var changed = false

        /* Perform one-time migration tasks on syncthing's config file when coming from an older config version. */
        changed = migrateSyncthingOptions()

        /* Get refs to important config objects */
        val folders = config!!.documentElement.getElementsByTagName("folder")

        /* Section - folders */
        for (i in 0..<folders.length) {
            val folder = folders.item(i) as Element
            // Set ignorePerms attribute.
            if (!folder.hasAttribute("ignorePerms") ||
                !folder.getAttribute("ignorePerms").toBoolean()
            ) {
                Log.i(TAG, "Set 'ignorePerms' on folder " + folder.getAttribute("id"))
                folder.setAttribute("ignorePerms", true.toString())
                changed = true
            }

            // Set 'hashers' (see https://github.com/syncthing/syncthing-android/issues/384) on the
            // given folder.
            changed = setConfigElement(folder, "hashers", "1") || changed
        }

        /* Section - GUI */
        val gui = this.guiElement

        // Platform-specific: Force REST API and Web UI access to use TLS 1.2 or not.
        val forceHttps = Constants.osSupportsTLS12()
        if (!gui.hasAttribute("tls") ||
            gui.getAttribute("tls").toBoolean() != forceHttps
        ) {
            gui.setAttribute("tls", if (forceHttps) "true" else "false")
            changed = true
        }

        // Set user to "syncthing"
        changed = setConfigElement(gui, "user", "syncthing") || changed

        // Set password to the API key
        var password = gui.getElementsByTagName("password").item(0)
        if (password == null) {
            password = config!!.createElement("password")
            gui.appendChild(password)
        }
        val apikey = this.apiKey
        val pw = password.textContent
        var passwordOk: Boolean
        try {
            passwordOk = !TextUtils.isEmpty(pw) && BCrypt.checkpw(apikey, pw)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Malformed password", e)
            passwordOk = false
        }
        if (!passwordOk) {
            Log.i(TAG, "Updating password")
            password.textContent = BCrypt.hashpw(apikey, BCrypt.gensalt(4))
            changed = true
        }

        /* Section - options */
        // Disable weak hash benchmark for faster startup.
        // https://github.com/syncthing/syncthing/issues/4348
        val options = config!!.documentElement
            .getElementsByTagName("options").item(0) as Element
        changed = setConfigElement(options, "weakHashSelectionMethod", "never") || changed

        /* Dismiss "fsWatcherNotification" according to https://github.com/syncthing/syncthing-android/pull/1051 */
        val childNodes = options.childNodes
        for (i in childNodes.length - 1 downTo 0) {
            val node = childNodes.item(i)

            if ("unackedNotificationID" == node.nodeName) {
                if ("fsWatcherNotification" == node.textContent) {
                    Log.i(TAG, "Remove found unackedNotificationID 'fsWatcherNotification'.")
                    options.removeChild(node)
                    changed = true
                }
            }
        }

        // Save changes if we made any.
        if (changed) {
            saveChanges()
        }
    }

    /**
     * Updates syncthing options to a version specific target setting in the config file.
     *
     * Used for one-time config migration from a lower syncthing version to the current version.
     * Enables filesystem watcher.
     * Returns if changes to the config have been made.
     */
    private fun migrateSyncthingOptions(): Boolean {
        /* Read existing config version */
        var iConfigVersion = config!!.documentElement.getAttribute("version").toInt()
        val iOldConfigVersion = iConfigVersion
        Log.i(TAG, "Found existing config version $iConfigVersion")

        /* Check if we have to do manual migration from version X to Y */
        if (iConfigVersion == 27) {
            /* fsWatcher transition - https://github.com/syncthing/syncthing/issues/4882 */
            Log.i(TAG, "Migrating config version $iConfigVersion to 28 ...")

            /* Enable fsWatcher for all folders */
            val folders = config!!.documentElement.getElementsByTagName("folder")
            for (i in 0..<folders.length) {
                val r = folders.item(i) as Element

                // Enable "fsWatcherEnabled" attribute and set default delay.
                Log.i(
                    TAG,
                    "Set 'fsWatcherEnabled', 'fsWatcherDelayS' on folder " + r.getAttribute("id")
                )
                r.setAttribute("fsWatcherEnabled", "true")
                r.setAttribute("fsWatcherDelayS", "10")
            }

            /*
            * Set config version to 28 after manual config migration
            * This prevents "unackedNotificationID" getting populated
            * with the fsWatcher GUI notification.
            */
            iConfigVersion = 28
        }

        if (iConfigVersion != iOldConfigVersion) {
            config!!.documentElement.setAttribute("version", iConfigVersion.toString())
            Log.i(TAG, "New config version is $iConfigVersion")
            return true
        } else {
            return false
        }
    }

    private fun setConfigElement(parent: Element, tagName: String?, textContent: String): Boolean {
        var element = parent.getElementsByTagName(tagName).item(0)
        if (element == null) {
            element = config!!.createElement(tagName)
            parent.appendChild(element)
        }
        if (textContent != element.textContent) {
            element.textContent = textContent
            return true
        }
        return false
    }

    private val guiElement: Element
        get() = config!!.documentElement.getElementsByTagName("gui")
            .item(0) as Element

    /**
     * Set device model name as device name for Syncthing.
     * 
     * We need to iterate through XML nodes manually, as mConfig.getDocumentElement() will also
     * return nested elements inside folder element. We have to check that we only rename the
     * device corresponding to the local device ID.
     * Returns if changes to the config have been made.
     */
    private fun changeLocalDeviceName(localDeviceID: String?): Boolean {
        val childNodes = config!!.documentElement.childNodes
        for (i in 0..<childNodes.length) {
            val node = childNodes.item(i)
            if (node.nodeName == "device") {
                if ((node as Element).getAttribute("id") == localDeviceID) {
                    Log.i(
                        TAG,
                        "changeLocalDeviceName: Rename device ID " + localDeviceID + " to " + Build.MODEL
                    )
                    node.setAttribute("name", Build.MODEL)
                    return true
                }
            }
        }
        return false
    }

    /**
     * Change default folder id to camera and path to camera folder path.
     * Returns if changes to the config have been made.
     */
    private fun changeDefaultFolder(): Boolean {
        val folder = config!!.documentElement
            .getElementsByTagName("folder").item(0) as Element
        val deviceModel = Build.MODEL
            .replace(" ", "_")
            .lowercase()
            .replace("[^a-z0-9_-]".toRegex(), "")
        val defaultFolderId = deviceModel + "_" + generateRandomString(FOLDER_ID_APPENDIX_LENGTH)
        folder.setAttribute("label", context.getString(R.string.default_folder_label))
        folder.setAttribute("id", context.getString(R.string.default_folder_id, defaultFolderId))
        folder.setAttribute(
            "path", Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath
        )
        folder.setAttribute("type", Constants.FOLDER_TYPE_SEND_ONLY)
        folder.setAttribute("fsWatcherEnabled", "true")
        folder.setAttribute("fsWatcherDelayS", "10")
        return true
    }

    /**
     * Generates a random String with a given length
     */
    private fun generateRandomString(length: Int): String {
        val chars = "abcdefghjkmnpqrstuvwxyz123456789".toCharArray()
        val random = Random()
        val sb = StringBuilder()
        for (i in 0..<length) {
            sb.append(chars[random.nextInt(chars.size)])
        }
        return sb.toString()
    }

    /**
     * Writes updated mConfig back to file.
     */
    private fun saveChanges() {
        if (!configFile.canWrite() && !fixAppDataPermissions(context)) {
            Log.w(TAG, "Failed to save updated config. Cannot change the owner of the config file.")
            return
        }

        Log.i(TAG, "Writing updated config file")
        val mConfigTempFile = Constants.getConfigTempFile(context)
        try {
            val transformerFactory = TransformerFactory.newInstance()
            val transformer = transformerFactory.newTransformer()
            val domSource = DOMSource(config)
            val streamResult = StreamResult(mConfigTempFile)
            transformer.transform(domSource, streamResult)
        } catch (e: TransformerException) {
            Log.w(TAG, "Failed to save temporary config file", e)
            return
        }
        try {
            if (!mConfigTempFile.renameTo(configFile)) {
                Log.w(TAG, "Failed to rename temporary config file to original file")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rename temporary config file to original file", e)
        }
    }

    companion object {
        private const val TAG = "ConfigXml"
        private const val FOLDER_ID_APPENDIX_LENGTH = 4
    }
}
