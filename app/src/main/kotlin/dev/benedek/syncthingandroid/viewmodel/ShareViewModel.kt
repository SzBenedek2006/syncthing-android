package dev.benedek.syncthingandroid.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel

class ShareViewModel : ViewModel() {
    val files: MutableMap<Uri, String> = HashMap()
}
