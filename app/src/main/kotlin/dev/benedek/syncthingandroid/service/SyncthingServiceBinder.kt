package dev.benedek.syncthingandroid.service

import android.os.Binder

class SyncthingServiceBinder(val service: SyncthingService) : Binder()
