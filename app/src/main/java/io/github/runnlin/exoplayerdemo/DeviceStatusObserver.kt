//package io.github.runnlin.exoplayerdemo
//
//import android.annotation.SuppressLint
//import android.content.Context
//import android.content.Intent
//import android.os.Environment
//import android.os.storage.StorageManager
//import android.os.storage.StorageVolume
//import android.util.Log
//import androidx.annotation.NonNull
//import io.github.runnlin.exoplayerdemo.data.MediaConstants.DevicePath.PATH_USB_A
//
//
//class DeviceStatusObserver(val mContext: Context, private val onUsbDiskMountState: ((Int, String) -> Unit)? = null) {
//    val TAG = this::class.simpleName
//    lateinit var storageManager: StorageManager
//    var isUsbMounted = false
//
//    init {
//        initStorageManager()
//    }
//
//    fun initStorageManager() {
//        try {
//            storageManager = mContext.getSystemService(Context.STORAGE_SERVICE) as StorageManager
//            storageManager.registerStorageVolumeCallback(mContext.mainExecutor, storageVolumeCallback)
//        }catch (e:Exception) {
//            Log.e(TAG, "StorageManager init failed")
//        }
//    }
//
//    private val storageVolumeCallback: StorageManager.StorageVolumeCallback =
//        object : StorageManager.StorageVolumeCallback() {
//            override fun onStateChanged(@NonNull volume: StorageVolume) {
//                super.onStateChanged(volume)
//                if (volume.isRemovable
//                    && null != volume.directory && volume.directory!!.absolutePath
//                        .equals(PATH_USB_A)
//                ) {
//                    Log.d(
//                        TAG, "VolumeCallback: "
//                                + volume + "    status: "
//                                + volume.state + "  isUsbMounted: "
//                                + isUsbMounted
//                    )
//                    when (volume.state) {
//                        Environment.MEDIA_MOUNTED ->
//                            // 原生会发送5条相同的信息，在这阻隔
//                            if (!isUsbMounted) {
//                                onUsbDiskMountState?.invoke(USBReceiver.USB_DISK_MOUNTED, PATH_USB_A)
//
//                                Log.d(
//                                    TAG, "on MOUNTED-----------:  "
//                                            + volume.state
//                                )
//                                isUsbMounted = true
//                            }
//                        Environment.MEDIA_EJECTING,
//                        Environment.MEDIA_UNKNOWN -> {
//                            onUsbDiskMountState?.invoke(USBReceiver.USB_DISK_UNMOUNTED, PATH_USB_A)
//                            isUsbMounted = false
//                        }
//                    }
//                }
//            }
//        }
//}