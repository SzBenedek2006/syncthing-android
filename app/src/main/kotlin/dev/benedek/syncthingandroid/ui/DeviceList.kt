package dev.benedek.syncthingandroid.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.activities.DeviceActivity
import dev.benedek.syncthingandroid.model.Device
import dev.benedek.syncthingandroid.model.DeviceStatuses
import dev.benedek.syncthingandroid.ui.theme.SyncthingandroidTheme
import dev.benedek.syncthingandroid.util.ThemeControls
import dev.benedek.syncthingandroid.util.Util.readableTransferRate
import dev.benedek.syncthingandroid.ui.theme.extendedColorScheme

@Composable
fun DeviceList(
    devices: List<Device>,
    deviceStatuses: DeviceStatuses,
    isLoaded: Boolean
) {
    if (!isLoaded) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (devices.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = stringResource(id = R.string.devices_list_empty))
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(
                items = devices,
                key = { device -> device.deviceID ?: device.hashCode() } // Maybe it helps performance? TODO: test
            ) { device ->
                deviceStatuses.connectionsMap?.get(device.deviceID)
                DeviceListItem(
                    device = device,
                    deviceStatus = deviceStatuses.connectionsMap?.get(device.deviceID)
                )
            }
        }
    }
}


@Composable
fun DeviceListItem(
    device: Device,
    deviceStatus: DeviceStatuses.DeviceStatus?
) {
    val context = LocalContext.current
    val localizedDeviceStatus = getLocalizedDeviceStatus(deviceStatus, context)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = device.displayName ?: device.name,
                onClick = {
                    val intent = Intent(context, DeviceActivity::class.java)
                    intent.putExtra(DeviceActivity.EXTRA_IS_CREATE, false)
                    intent.putExtra(DeviceActivity.EXTRA_DEVICE_ID, device.deviceID)
                    context.startActivity(intent)
                }
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = device.displayName ?: device.name ?: device.deviceID ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )

                Text(
                    text = localizedDeviceStatus.status,
                    style = MaterialTheme.typography.labelMedium,
                    color = localizedDeviceStatus.color
                )
            }

            Row {
                Text(
                    text = stringResource(R.string.download_title_colon) + " ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = readableTransferRate(context, deviceStatus?.inBits ?: 0L),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row {
                Text(
                    text = stringResource(R.string.upload_title_colon) + " ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = readableTransferRate(context, deviceStatus?.outBits ?: 0L),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

data class LocalizedDeviceStatus(
    var download: String = "",
    var upload: String = "",
    var status: String = "",
    var color: Color = Color.Unspecified
)

@Composable
fun getLocalizedDeviceStatus(
    deviceStatus: DeviceStatuses.DeviceStatus?,
    context: Context
): LocalizedDeviceStatus {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val blue = MaterialTheme.extendedColorScheme.blue.color
    val green = MaterialTheme.extendedColorScheme.green.color
    val red = MaterialTheme.extendedColorScheme.red.color



    return remember(deviceStatus, context) {
        val localizedDeviceStatus = LocalizedDeviceStatus()
        if (deviceStatus == null) {
            localizedDeviceStatus.download = readableTransferRate(context, 0)
            localizedDeviceStatus.upload = readableTransferRate(context, 0)
            localizedDeviceStatus.status = context.getString(R.string.device_state_unknown)
            localizedDeviceStatus.color = red

            localizedDeviceStatus
        } else if (deviceStatus.paused) {
            localizedDeviceStatus.download = readableTransferRate(context, 0)
            localizedDeviceStatus.upload = readableTransferRate(context, 0)
            localizedDeviceStatus.status = context.getString(R.string.device_paused)
            localizedDeviceStatus.color = onSurface

            localizedDeviceStatus
        } else if (deviceStatus.connected) {
            localizedDeviceStatus.download = readableTransferRate(context, deviceStatus.inBits)
            localizedDeviceStatus.upload = readableTransferRate(context, deviceStatus.outBits)
            if (deviceStatus.completion == 100) {
                localizedDeviceStatus.status = context.getString(R.string.device_up_to_date)
                localizedDeviceStatus.color = green
            } else {
                localizedDeviceStatus.status = context.getString(R.string.device_syncing, deviceStatus.completion)
                localizedDeviceStatus.color = blue
            }

            localizedDeviceStatus
        } else {
            localizedDeviceStatus.download = readableTransferRate(context, 0)
            localizedDeviceStatus.upload = readableTransferRate(context, 0)
            localizedDeviceStatus.status = context.getString(R.string.device_disconnected)
            localizedDeviceStatus.color = red

            localizedDeviceStatus
        }
    }

}

@Preview(uiMode = ThemeControls.uiMode)
@Composable
fun DeviceListPreview() {
    SyncthingandroidTheme(dynamicColor = ThemeControls.blurEnabled, darkTheme = ThemeControls.previewDarkTheme) {
        Surface { DeviceList(emptyList(), DeviceStatuses(), true) }
    }
}

@Preview(uiMode = ThemeControls.uiMode)
@Composable
fun DeviceListItemPreview() {
    SyncthingandroidTheme(dynamicColor = ThemeControls.blurEnabled, darkTheme = ThemeControls.previewDarkTheme) {
        Surface{ DeviceListItem(Device(), DeviceStatuses.DeviceStatus()) }
    }
}