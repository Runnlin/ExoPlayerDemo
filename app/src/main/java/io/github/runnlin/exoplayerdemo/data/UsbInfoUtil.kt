package io.github.runnlin.exoplayerdemo.data


import android.content.Context
import android.os.StatFs
import android.os.storage.StorageManager
import android.text.TextUtils
import android.util.Log
import io.github.runnlin.exoplayerdemo.UEventReceiver
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.io.Reader


object UsbInfoUtil {
    private const val TAG = "UsbInfoUtil"
    private const val PATH_PRODUCT_ID = "/idProduct"
    private const val PATH_VENDOR_ID = "/idVendor"
    private const val PATH_SERIAL_NUM = "/serial"
    private const val PATH_INTERFACE = "/bInterfaceClass"
    private const val PATH_DEV_NUM = "/devnum"
    private const val INTERFACE_HID = 0x03
    private const val INTERFACE_PRINTER = 0x07
    private const val INTERFACE_USB = 0x08
    private const val INTERFACE_HUB = 0x09
    private const val INTERFACE_VIDEO = 0x0E
    private const val APPLE_VENDOR_ID = 0x05AC
    private const val APPLE_PRODUCT_ID = 0x1200
    private const val APPLE_PRODUCT_ID_MASK = 0xFF00
//    fun getUsbStorageStatus(context: Context, mountPath: String): String {
//        Log.d(TAG, "getUsbStorageStatus: mountPath == mountPath")
////        val last = getLastUsbInfo(context)
//        val current = getCurrentUsbInfo(context, mountPath)
//        if (last != null) {
//            Log.d(
//                TAG,
//                "getUsbStorageStatus: current = " + current.serialNum + " -- last = " + last.serialNum
//            )
//            Log.d(
//                TAG,
//                "getUsbStorageStatus: current = " + current.productId + " -- last = " + last.productId
//            )
//            Log.d(
//                TAG,
//                "getUsbStorageStatus: current = " + current.vendorId + " -- last = " + last.vendorId
//            )
//            Log.d(
//                TAG,
//                "getUsbStorageStatus: current = " + current.totalBytes + " -- last = " + last.totalBytes
//            )
//            Log.d(
//                TAG,
//                "getUsbStorageStatus: current = " + current.freeBytes + " -- last = " + last.freeBytes
//            )
//            if (isCurrentUsbEqualsLast(current, last)) {
//                Log.d(TAG, "getUsbStorageStatus: current == last")
////                return getUsbStorageStatus(context)
//            } else if (current.totalBytes == 0L) {
//                Log.w(TAG, "getUsbStorageStatus: current status unknown")
//                return MediaConstants.DeviceStatus.STATUS_UNKNOWN
//            }
//        }
//        Log.d(TAG, "getUsbStorageStatus: current != last")
//        saveUsbStorageStatus(context, MediaConstants.DeviceStatus.STATUS_MOUNTED)
//        return MediaConstants.DeviceStatus.STATUS_MOUNTED
//    }

    private fun getCurrentUsbInfo(context: Context, mountPath: String): UsbInfo {
        return try {
            //get the currently mounted sysPath
            var mountedSysPath: String? = null
            val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
            val storeManagerClazz = Class.forName("android.os.storage.StorageManager")
            val getVolumesMethod = storeManagerClazz.getMethod("getVolumes")
            val volumeInfos = getVolumesMethod.invoke(storageManager) as List<*>
            val volumeInfoClazz = Class.forName("android.os.storage.VolumeInfo")
            val diskInfoClazz = Class.forName("android.os.storage.DiskInfo")
            val getDiskInfoMethod = volumeInfoClazz.getMethod("getDisk")
            val fsLabelField = volumeInfoClazz.getDeclaredField("fsLabel")
            val pathField = volumeInfoClazz.getDeclaredField("path")
            val sysPathField = diskInfoClazz.getDeclaredField("sysPath")
            if (volumeInfos != null) {
                for (volumeInfo in volumeInfos) {
                    val fsLabelString = fsLabelField[volumeInfo] as String
                    val pathString = pathField[volumeInfo] as String
                    Log.d(TAG, "getCurrentUsbInfo: fsLabelString = $fsLabelString")
                    Log.d(TAG, "getCurrentUsbInfo: pathString = $pathString")
                    val diskInfo = getDiskInfoMethod.invoke(volumeInfo)
                    if (diskInfo != null && mountPath == pathString) {
                        mountedSysPath = sysPathField[diskInfo] as String
                        Log.d(
                            TAG,
                            "getCurrentUsbInfo: mountedSysPath = $mountedSysPath"
                        )
                        break
                    }
                }
            }

            //get the currently mounted UsbInfo
            var info: UsbInfo? = null
            if (!TextUtils.isEmpty(mountedSysPath)) {
                if (mountedSysPath!!.contains("//")) {
                    mountedSysPath = mountedSysPath.replace("//", "/")
                    Log.d(
                        TAG,
                        "getCurrentUsbInfo: mountedSysPath = $mountedSysPath"
                    )
                }
                for (i in 0 until UEventReceiver.getInstance().getUsbStorage().size()) {
                    val usbInfo: UsbInfo = UEventReceiver.getInstance().getUsbStorage().valueAt(i)
                    if (mountedSysPath.contains(usbInfo.devPath)) {
                        info = usbInfo
                        break
                    }
                }
            }
            if (info == null) {
                Log.e(TAG, "getCurrentUsbInfo: cannot find current mounted usbInfo!")
                return UsbInfo()
            }
            val devPath = info.devPath
            info.isMounded = true
            Log.d(TAG, "getCurrentUsbInfo: devPath = $devPath")

            //get current usb storage serial number
            val serialNum = getSerialNum(devPath)
            info.serialNum = serialNum

            //get current usb storage size
            val statFs = StatFs(mountPath)
            val totalBytes = statFs.totalBytes
            val freeBytes = statFs.freeBytes
            val blockBytes = statFs.blockSizeLong
            info.totalBytes = totalBytes
            info.freeBytes = freeBytes
            info.blockBytes = blockBytes
            getAutoCreateFileSize(mountPath, info)

            //save current usb storage info
            saveUsbStorageInfo(context, info)
            info
        } catch (e: Exception) {
            e.printStackTrace()
            UsbInfo()
        }
    }

    private fun isCurrentUsbEqualsLast(current: UsbInfo, last: UsbInfo): Boolean {
        val curSerialNum = if (current.serialNum == null) "" else current.serialNum!!
        val lastSerialNum = if (last.serialNum == null) "" else last.serialNum!!
        if (curSerialNum != lastSerialNum || current.vendorId != last.vendorId || current.productId != last.productId) {
            Log.i(TAG, "isCurrentUsbEqualsLast: The usb device is different")
            return false
        }
        if (current.totalBytes != last.totalBytes) {
            Log.i(TAG, "isCurrentUsbEqualsLast: The TotalBytes is different")
            return false
        }
        if (current.blockBytes != last.blockBytes) {
            Log.i(TAG, "isCurrentUsbEqualsLast: The BlockBytes is different")
            return false
        }
        if (current.freeBytes != last.freeBytes) {
            Log.i(TAG, "isCurrentUsbEqualsLast: The FreeBytes is different")
            val currentCreate = current.createBytes + current.createCount * current.blockBytes
            val lastCreate = last.createBytes + last.createCount * last.blockBytes
            val currentFree = current.freeBytes + currentCreate
            val lastFree = last.freeBytes + lastCreate
            val offsetBytes = Math.abs(currentFree - lastFree)
            val offsetCount = offsetBytes / current.blockBytes
            Log.d(
                TAG,
                "isCurrentUsbEqualsLast: currentFree = $currentFree -- lastFree = $lastFree"
            )
            Log.i(
                TAG,
                "isCurrentUsbEqualsLast: offsetBytes = $offsetBytes; offsetCount = $offsetCount"
            )
            return offsetCount < 5 //计算偏差小于5
        }
        return true
    }

    private fun getAutoCreateFileSize(mountPath: String, usbInfo: UsbInfo) {
        val pathAndroid = "$mountPath/Android"
        val pathLOST = "$mountPath/LOST.DIR"
        val fileAndroid = File(pathAndroid)
        val fileLOST = File(pathLOST)
        usbInfo.createBytes = 0
        usbInfo.createCount = 0
        getAutoCreateFileSize(fileAndroid, usbInfo)
        getAutoCreateFileSize(fileLOST, usbInfo)
//        Log.d(
//            TAG, "getAutoCreateFileSize: CreateBytes = %d + CreateCount = %d",
//            usbInfo.createBytes, usbInfo.createCount
//        )
    }

    private fun getAutoCreateFileSize(file: File?, usbInfo: UsbInfo) {
        if (file == null || !file.exists()) {
            return
        }
        usbInfo.createCount = usbInfo.createCount + 1
        try {
            val files = file.listFiles()
            for (f in files) {
                if (f.isDirectory) {
                    getAutoCreateFileSize(f, usbInfo)
                } else {
                    usbInfo.createBytes = usbInfo.createBytes + f.length()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

//    private fun getLastUsbInfo(context: Context): UsbInfo? {
////        val memoryObj: Any = ShareMemoryUtil.getObject(context, ShareMemoryUtil.KEY_USB_INFO)
//        return if (memoryObj is UsbInfo) {
//            memoryObj
//        } else null
//    }

//    private fun getUsbStorageStatus(context: Context): String {
////        return ShareMemoryUtil.getString(context, ShareMemoryUtil.KEY_USB_STATUS)
//    }

    private fun saveUsbStorageInfo(context: Context, usbInfo: UsbInfo) {
        Log.d(TAG, "saveUsbStorageInfo: usbInfo = $usbInfo")
        try {
//            ShareMemoryUtil.saveObject(context, ShareMemoryUtil.KEY_USB_INFO, usbInfo)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveUsbStorageStatus(context: Context?, status: String) {
        Log.d(TAG, "saveUsbStorageStatus: status = $status")
//        ShareMemoryUtil.saveString(context, ShareMemoryUtil.KEY_USB_STATUS, status)
    }

    fun isHidDevice(bInterfaceClass: Int): Boolean {
        val isHidDevice = bInterfaceClass == INTERFACE_HID
        Log.d(TAG, "isHidDevice = $isHidDevice")
        return isHidDevice
    }

    fun isPrinterDevice(bInterfaceClass: Int): Boolean {
        val isPrinterDevice = bInterfaceClass == INTERFACE_PRINTER
        Log.d(TAG, "isPrinterDevice = $isPrinterDevice")
        return isPrinterDevice
    }

    fun isHubDevice(bInterfaceClass: Int): Boolean {
        val isHubDevice = bInterfaceClass == INTERFACE_HUB
        Log.d(TAG, "isHubDevice = $isHubDevice")
        return isHubDevice
    }

    fun isVideoDevice(bInterfaceClass: Int): Boolean {
        val isVideoDevice = bInterfaceClass == INTERFACE_VIDEO
        Log.d(TAG, "isVideoDevice = $isVideoDevice")
        return isVideoDevice
    }

    fun isUsbStorage(bInterfaceClass: Int): Boolean {
        val isUsbStorage = bInterfaceClass == INTERFACE_USB
        Log.d(TAG, "isUsbStorage = $isUsbStorage")
        return isUsbStorage
    }

    fun isIPodDevice(idVendor: Int, idProduct: Int): Boolean {
        val isAppleDevice = idVendor == APPLE_VENDOR_ID &&
                idProduct and APPLE_PRODUCT_ID_MASK == APPLE_PRODUCT_ID
        Log.d(TAG, "isIPodDevice = $isAppleDevice")
        return isAppleDevice
    }

    fun getProductId(path: String): Int {
        val pidPath = path + PATH_PRODUCT_ID
        return readFileString(pidPath, 4)!!.toInt(16)
    }

    fun getVendorId(path: String): Int {
        val vidPath = path + PATH_VENDOR_ID
        return readFileString(vidPath, 4)!!.toInt(16)
    }

    fun getInterfaceClass(path: String): Int {
        val interfacePath = path + PATH_INTERFACE
        return readFileString(interfacePath, 2)!!.toInt(16)
    }

    fun getDeviceNum(path: String): Int {
        val pidPath = path + PATH_DEV_NUM
        return readFileString(pidPath, 3)!!.toInt()
    }

    private fun getSerialNum(path: String): String? {
        val serialNumPath = path + PATH_SERIAL_NUM
        return readFileString(serialNumPath, 24)
    }

    private fun readFileString(filePath: String, length: Int): String? {
        if (!File(filePath).exists()) {
            Log.e(TAG, "readFileString: file is not exists, path = $filePath")
            return null
        }
        var reader: Reader? = null
        val buff = CharArray(length)
        try {
            reader = InputStreamReader(FileInputStream(filePath))
            if (reader.read(buff) == -1) {
                Log.e(TAG, "readFileString: cannot read file!")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (reader != null) {
                try {
                    reader.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        var size = 0
        val output: String
        for (c in buff) {
            if (c.code == 10) break
            size++
        }
        output = String(buff).substring(0, size)
        Log.i(
            TAG, "readFileString: output.length = " + output.length
                    + "; output = " + output
        )
        return output
    }
}
