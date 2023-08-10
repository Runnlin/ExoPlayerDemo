//package io.github.runnlin.exoplayerdemo
//
//import android.annotation.SuppressLint
//import android.content.Context
//import android.content.Intent
//import android.os.Bundle
//import android.os.Handler
//import android.os.Looper
//import android.os.UEventObserver
//import android.security.KeyStore.getApplicationContext
//import android.util.Log
//import android.util.SparseArray
//import android.widget.Toast
//import io.github.runnlin.exoplayerdemo.data.MediaService
//import io.github.runnlin.exoplayerdemo.data.UsbInfo
//import io.github.runnlin.exoplayerdemo.data.UsbInfoUtil
//import java.io.File
//import java.util.*
//import kotlin.collections.HashMap
//import kotlin.concurrent.schedule
//
//
//private const val USB_STATE_MATCH = "DEVPATH=/"
//private const val PATH_SYS = "/sys";
//
//private const val PATH_DEV_HIGH = "/devices/platform/soc/a600000.ssusb/a600000.dwc3"
//private const val PATH_DEV_LOW = "/devices/platform/soc/ee080000.usb/usb3/3-1"
//private const val PATH_NO_RESPOND = "/1-1"
//
//private const val KEY_U_EVENT_ACTION = "ACTION"
//private const val KEY_U_EVENT_PRODUCT = "PRODUCT"
//private const val KEY_U_EVENT_DEV_PATH = "DEVPATH"
//private const val KEY_U_EVENT_INTERFACE = "INTERFACE"
//private const val KEY_U_EVENT_DEV_NAME = "DEVNAME"
//
//private const val VALUE_U_EVENT_ADD = "add"
//private const val VALUE_U_EVENT_REMOVE = "remove"
//private const val VALUE_U_EVENT_CHANGE = "change"
//
//private const val TYPE_HID = "hid"
//private const val TYPE_UNKNOWN = "unknown"
//private const val TYPE_PRINTER = "printer"
//private const val TYPE_HUB_DEVICE = "hub"
//private const val TYPE_USB_STORAGE = "usb_storage"
//private const val TYPE_VIDEO = "video"
//private const val TYPE_IPOD_DEVICE = "iPod"
//
//
//private const val TAG = "ZRL|UEventReceiver"
//
//
//private val mLastDevice: HashMap<String, String> = HashMap()
//
///**
// * String = device node;
// * UsbInfo = usb storage info;
// */
//var mUsbStorage: SparseArray<UsbInfo> = SparseArray()
//
//private val mDevices: SparseArray<String> = SparseArray()
//
//private var mHubCounter = 0
//
//
//open class UEventReceiver : UEventObserver() {
//
//    private var mContext: Context? = null
//
////    private var mainActivity:MainActivity? =null
////    private var mainActivity = MainActivity.getInstance()
//
//    override fun onUEvent(uEvent: UEvent?) {
//        if (uEvent != null) {
//            Log.i(TAG, "onUEvent: uEvent = $uEvent")
//            handleUEvent(uEvent)
//        }
//    }
//
//    companion object {
//        fun getInstance() = Helper.instance
//    }
//
//    private object Helper {
//        @SuppressLint("StaticFieldLeak")
//        val instance = UEventReceiver()
//    }
//
//    fun init(context: Context) {
//        mContext = context
////        mainActivity = MainActivity.getInstance()
//        startObserving(USB_STATE_MATCH)
//    }
//
//    fun destroy() {
//        mContext = null
//        stopObserving()
//    }
//
//
//    open fun handleUEvent(uEvent: UEvent) {
//        //LogUtil.d(TAG, "handleUEvent: uEvent = " + uEvent);
//        val devPath = uEvent[KEY_U_EVENT_DEV_PATH]
//        when (uEvent[KEY_U_EVENT_ACTION]) {
//            VALUE_U_EVENT_ADD ->
//                if ((devPath.contains(PATH_DEV_HIGH) ||
//                            devPath.contains(PATH_DEV_LOW)) && !devPath.contains(":")
//                ) {
//                    Log.d(TAG, "onUEvent1: add --> $uEvent")
//                    if (uEvent.get(KEY_U_EVENT_PRODUCT) != null) {
//                        mLastDevice[uEvent.get(KEY_U_EVENT_PRODUCT)] = uEvent[KEY_U_EVENT_DEV_NAME]
//                    }
//
//                } else if ((devPath.contains(PATH_DEV_HIGH) || devPath.contains(PATH_DEV_LOW))
//                    && uEvent[KEY_U_EVENT_INTERFACE] != null
//                    && mLastDevice.containsKey(uEvent[KEY_U_EVENT_PRODUCT])
//                ) {
//                    Log.d(TAG, "onUEvent2: add --> $uEvent")
//                    handleUEventAdd(uEvent)
//                    mLastDevice.remove(uEvent[KEY_U_EVENT_PRODUCT])
//                }
//
//            VALUE_U_EVENT_REMOVE ->
//                if ((devPath.contains(PATH_DEV_HIGH) || devPath.contains(PATH_DEV_LOW))
//                    && !devPath.contains(":")
//                ) {
//                    Log.d(TAG, "onUEvent: remove --> $uEvent")
//                    handleUEventRemove(uEvent)
//                }
//            else -> {}
//        }
//    }
//
//    private fun handleUEventAdd(uEvent: UEvent) {
//        Log.d(TAG, "handleUEventAdd")
//        var mDevPath = uEvent[KEY_U_EVENT_DEV_PATH]
//        mDevPath = PATH_SYS + mDevPath.substring(0, mDevPath.lastIndexOf("/"))
//        val mProduct = uEvent[KEY_U_EVENT_PRODUCT]
//        val mInterface = uEvent[KEY_U_EVENT_INTERFACE]
//        val mDevName: String? = mLastDevice.get(mProduct)
//        val id = mProduct.split("/").toTypedArray()
//        val idVendor = id[0].toInt(16)
//        val idProduct = id[1].toInt(16)
//        val bInterfaceClass = mInterface.split("/").toTypedArray()[0].toInt(16)
//        val devNum = mDevName?.substring(mDevName.lastIndexOf("/") + 1)?.toInt()
//        Log.i(
//            TAG,
//            "handleUEventAdd: idVendor = $idVendor; idProduct = $idProduct; " +
//                    "bInterfaceClass = $bInterfaceClass; devNum = $devNum"
//
//        )
//        if (devNum != null) {
//            if (UsbInfoUtil.isUsbStorage(bInterfaceClass)) {
//                val usbInfo = UsbInfo()
//                usbInfo.devPath = mDevPath
//                usbInfo.vendorId = idVendor
//                usbInfo.productId = idProduct
//
//                mUsbStorage.put(devNum, usbInfo)
//                handleDeviceType(devNum, TYPE_USB_STORAGE, true)
//            } else if (UsbInfoUtil.isHubDevice(bInterfaceClass)) {
//                handleDeviceType(devNum, TYPE_HUB_DEVICE, true)
//            } else if (UsbInfoUtil.isIPodDevice(idVendor, idProduct)) {
//                handleDeviceType(devNum, TYPE_IPOD_DEVICE, true)
//            } else if (UsbInfoUtil.isHidDevice(bInterfaceClass)) {
//                handleDeviceType(devNum, TYPE_HID, true)
//            } else if (UsbInfoUtil.isVideoDevice(bInterfaceClass)) {
//                handleDeviceType(devNum, TYPE_VIDEO, true)
//            } else if (UsbInfoUtil.isPrinterDevice(bInterfaceClass)) {
//                handleDeviceType(devNum, TYPE_PRINTER, true)
//            } else {
//                Log.i(
//                    TAG, "Unknown device: idVendor = $idVendor; idProduct = $idProduct; " +
//                            "bInterfaceClass = $bInterfaceClass; devNum = $devNum"
//                )
//            }
//        }
//    }
//
//    /**
//     * handle Device Type
//     *
//     * @param devNum device number
//     * @param type   device type: TYPE_HUB_DEVICE / TYPE_USB_STORAGE / TYPE_IPOD_DEVICE / TYPE_UNKNOWN
//     * @param show   true: show toast.
//     */
//    private fun handleDeviceType(devNum: Int, type: String, show: Boolean) {
//        Log.d(TAG, "handleDeviceType: devNum = $devNum; type = $type")
//        Log.d(TAG, "handleDeviceType: show = " + show + "; mDevCounter = " + mUsbStorage.size())
//        Log.d(
//            TAG, "handleDeviceType: mUsbStorage = " + mUsbStorage.size() +
//                    "; mDevices = " + mDevices.size()
//        )
//        mDevices.put(devNum, type)
//        val handler = Handler(Looper.getMainLooper())
//        when (type) {
//            TYPE_HUB_DEVICE -> {
//                mHubCounter++
//                if (show && mHubCounter == 1) {
////                    ToastUtil.getInstance().show(mContext, R.string.media_service_dev_hub);
//                    handler.post {
//                        Toast.makeText(
//                            mContext,
//                            "hub",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                    }
//                } else if (show && mHubCounter > 1) {
////                    ToastUtil.getInstance().show(mContext, R.string.media_service_hub_too_deep);
//                    handler.post {
//                        Toast.makeText(
//                            mContext,
//                            "节点太多",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                    }
//                    mHubCounter = 2
//                }
//            }
//            TYPE_USB_STORAGE -> {
//                Log.d(TAG, "handleDeviceType mDevCounter up")
//                if (show && mUsbStorage.size() == 1) {
////                    ToastUtil.getInstance().show(mContext, R.string.media_service_dev_loading);
//
//                    handler.post {
//                        Toast.makeText(
//                            mContext,
//                            "u盘载入中",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                    }
////                    handler.postDelayed({
////                    }, 3000)
//                } else if (show && mUsbStorage.size() >= 2) {
////                    ToastUtil.getInstance().show(mContext, R.string.media_service_dev_exceeded);
//                    handler.post {
//                        Toast.makeText(
//                            mContext,
//                            "u盘太多",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                    }
//                }
//            }
//            TYPE_HID, TYPE_VIDEO, TYPE_PRINTER ->
//                handler.post {
//                    Toast.makeText(
//                        mContext,
//                        "不支持的设备",
//                        Toast.LENGTH_LONG
//                    ).show()
//                }
//        }
//        Log.i(
//            TAG, "handleDeviceType: mHubCounter = $mHubCounter; mDevCounter = ${mUsbStorage.size()}"
//        )
//    }
//
//    private fun handleUEventRemove(uEvent: UEvent) {
//        val mDevName = uEvent[KEY_U_EVENT_DEV_NAME]
//        val devNum = mDevName.substring(mDevName.lastIndexOf("/") + 1).toInt()
//        Log.i(TAG, "handleUEventRemove: devNum = $devNum")
//        Log.i(TAG, "handleUEventRemove mDevices = $mDevices")
//        if (null == mDevices || 0 == mDevices.size()) {
//            return
//        }
//        val deviceType: String = mDevices.get(devNum)
//        val handler = Handler(Looper.getMainLooper())
//        if (deviceType == null) {
//            Log.e(TAG, "handleUEventRemove deviceType = null ")
//            //            ToastUtil.getInstance().show(mContext, R.string.media_service_dev_remove);
//            handler.post(Runnable {
//                Toast.makeText(
//                    mContext,
//                    "设备已移除",
//                    Toast.LENGTH_SHORT
//                ).show()
//            })
//            return
//        }
//        when (deviceType) {
//            TYPE_HUB_DEVICE -> {
//                if (--mHubCounter < 0) mHubCounter = 0
//                //                ToastUtil.getInstance().show(mContext, R.string.media_service_dev_remove);
//                handler.post(Runnable {
//                    Toast.makeText(
//                        mContext,
//                        "设备已移除",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                })
//            }
//            TYPE_USB_STORAGE -> {
//                //                if (mDevCounter > 0) {
////                    mDevCounter--;
////                } else {
////                    mDevCounter = 0;
////                }
//                mUsbStorage.remove(devNum)
//
//                Log.d(TAG, "handleUEventRemove mDevCounter down")
//                //            case TYPE_IPOD_DEVICE:
////                ToastUtil.getInstance().show(mContext, R.string.media_service_dev_remove);
//                handler.post(Runnable {
//                    Toast.makeText(
//                        mContext,
//                        "设备已移除",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                })
//                var intent = Intent(mContext, MediaService::class.java)
//                var bundle = Bundle()
//                mContext?.startService(Intent(mContext, MediaService::class.java))
//            }
//            else -> {}
//        }
//        mDevices.remove(devNum)
//        Log.i(
//            TAG,
//            "handleUEventRemove: mHubCounter = $mHubCounter; mDevCounter = ${mUsbStorage.size()}"
//        )
//    }
//
//
//    open fun getUsbStorage(): SparseArray<UsbInfo> {
//        return mUsbStorage
//    }
//}