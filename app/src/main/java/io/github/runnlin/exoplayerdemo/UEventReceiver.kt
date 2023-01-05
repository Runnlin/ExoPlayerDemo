package io.github.runnlin.exoplayerdemo

import android.annotation.SuppressLint
import android.content.Context
import android.os.UEventObserver
import android.util.Log

private const val USB_STATE_MATCH = "DEVPATH=/"

private const val KEY_U_EVENT_ACTION = "ACTION"
private const val KEY_U_EVENT_PRODUCT = "PRODUCT"
private const val KEY_U_EVENT_DEV_PATH = "DEVPATH"
private const val KEY_U_EVENT_INTERFACE = "INTERFACE"
private const val KEY_U_EVENT_DEV_NAME = "DEVNAME"

private const val VALUE_U_EVENT_ADD = "add"
private const val VALUE_U_EVENT_REMOVE = "remove"
private const val VALUE_U_EVENT_CHANGE = "change"

private const val TYPE_HID = "hid"
private const val TYPE_UNKNOWN = "unknown"
private const val TYPE_PRINTER = "printer"
private const val TYPE_HUB_DEVICE = "hub"
private const val TYPE_USB_STORAGE = "usb_storage"
private const val TYPE_VIDEO = "video"
private const val TYPE_IPOD_DEVICE = "iPod"

open class UEventReceiver: UEventObserver() {

    private var mContext: Context?=null

    override fun onUEvent(uEvent: UEvent?) {
        if (uEvent != null) {
            Log.i("ZRL|UEventReceiver", "onUEvent: uEvent = $uEvent")
        }
    }

    companion object {
        fun getInstance() = Helper.instance
    }
    private object Helper {
        @SuppressLint("StaticFieldLeak")
        val instance = UEventReceiver()
    }

    fun init(context: Context) {
        mContext = context
        startObserving(USB_STATE_MATCH)
    }

    fun destroy() {
        mContext = null
        stopObserving()
    }
}