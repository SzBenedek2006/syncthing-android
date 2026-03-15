package dev.benedek.syncthingandroid.views

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.color.MaterialColors
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.model.Connections
import dev.benedek.syncthingandroid.model.Device
import dev.benedek.syncthingandroid.service.RestApi
import dev.benedek.syncthingandroid.util.Util

/**
 * Generates item views for device items.
 */
class DevicesAdapter(context: Context) : ArrayAdapter<Device?>(context, R.layout.item_device_list) {
    private var connections: Connections? = null

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        if (convertView == null) {
            val inflater = context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertView = inflater.inflate(R.layout.item_device_list, parent, false)
        }

        val name = convertView.findViewById<TextView>(R.id.name)
        val status = convertView.findViewById<TextView>(R.id.status)
        val download = convertView.findViewById<TextView>(R.id.download)
        val upload = convertView.findViewById<TextView>(R.id.upload)

        val deviceId = getItem(position)!!.deviceID

        name.text = getItem(position)!!.displayName
        val r = context.resources

        var conn: Connections.Connection? = null
        if (connections != null && connections!!.connectionsMap?.containsKey(deviceId) == true) {
            conn = connections!!.connectionsMap!![deviceId]
        }

        if (conn == null) {
            download.text = Util.readableTransferRate(context, 0)
            upload.text = Util.readableTransferRate(context, 0)
            status.text = r.getString(R.string.device_state_unknown)
            status.setTextColor(ContextCompat.getColor(context, R.color.text_red))
            return convertView
        }

        if (conn.paused) {
            download.text = Util.readableTransferRate(context, 0)
            upload.text = Util.readableTransferRate(context, 0)
            status.text = r.getString(R.string.device_paused)
            status.setTextColor(
                MaterialColors.getColor(
                    context,
                    android.R.attr.textColorPrimary,
                    Color.BLACK
                )
            )
            return convertView
        }

        if (conn.connected) {
            download.text = Util.readableTransferRate(context, conn.inBits)
            upload.text = Util.readableTransferRate(context, conn.outBits)
            if (conn.completion == 100) {
                status.text = r.getString(R.string.device_up_to_date)
                status.setTextColor(ContextCompat.getColor(context, R.color.text_green))
            } else {
                status.text = r.getString(R.string.device_syncing, conn.completion)
                status.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.md_theme_onPrimaryContainer
                    )
                )
            }
            return convertView
        }

        // !conn.connected
        download.text = Util.readableTransferRate(context, 0)
        upload.text = Util.readableTransferRate(context, 0)
        status.text = r.getString(R.string.device_disconnected)
        status.setTextColor(ContextCompat.getColor(context, R.color.text_red))
        return convertView
    }

    /**
     * Requests new connection info for all devices visible in listView.
     */
    fun updateConnections(api: RestApi) {
        for (i in 0..<count) {
            api.getConnections { connections: Connections? ->
                this.onReceiveConnections(
                    connections
                )
            }
        }
    }

    private fun onReceiveConnections(connections: Connections?) {
        this.connections = connections
        notifyDataSetChanged()
    }
}
