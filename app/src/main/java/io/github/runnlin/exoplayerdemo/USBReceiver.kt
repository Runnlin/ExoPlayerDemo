package io.github.runnlin.exoplayerdemo

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class USBReceiver(private val onUsbDiskMountState: ((Int) -> Unit)? = null) : BroadcastReceiver() {

    companion object {
        private val TAG = USBReceiver::class.java.simpleName
        const val USB_DISK_MOUNTED = 1
        const val USB_DISK_UNMOUNTED = 2
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val _storageManager = context?.getSystemService(Activity.STORAGE_SERVICE)
        val _action = intent?.action
        Log.d(TAG, "onReceiver: $_action")

        if (null != _storageManager && null != _action) {
            when (_action) {
                Intent.ACTION_MEDIA_MOUNTED -> {
                    onUsbDiskMountState?.invoke(USB_DISK_MOUNTED)
                    Log.d(TAG, "USB UNMounted")
                }
                Intent.ACTION_MEDIA_UNMOUNTED, Intent.ACTION_MEDIA_EJECT -> {
                    onUsbDiskMountState?.invoke(USB_DISK_UNMOUNTED)
                    Log.d(TAG, "USB UNMounted")
                }
            }
        }
    }
}