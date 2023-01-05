package io.github.runnlin.exoplayerdemo.data

import android.os.Parcel
import android.os.Parcelable
import java.util.*

data class UsbInfo(
    var devPath: String = "",
    var serialNum: String? = "", //U盘唯一标识符,
    var productId: Int = 0, //U盘产品ID,
    var vendorId: Int = 0, //U盘厂商ID,
    var totalBytes: Long = 0, //U盘总容量
    var freeBytes: Long = 0, //U盘剩余容量
    var blockBytes: Long = 0, //U盘分配单元大小
    var createBytes: Long = 0, //U盘自动生成的文件大小
    var createCount: Int = 0, //U盘自动生成的文件夹子个数
    var isMounded: Boolean = false) //U盘是否已挂载) : Parcelable, Serializable
    {


    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val usbInfo = o as UsbInfo
        return productId == usbInfo.productId && vendorId == usbInfo.vendorId && totalBytes == usbInfo.totalBytes && freeBytes == usbInfo.freeBytes && blockBytes == usbInfo.blockBytes && createBytes == usbInfo.createBytes && createCount == usbInfo.createCount && isMounded == usbInfo.isMounded &&
                devPath == usbInfo.devPath &&
                serialNum == usbInfo.serialNum
    }

    override fun hashCode(): Int {
        return Objects.hash(
            devPath,
            serialNum,
            productId,
            vendorId,
            totalBytes,
            freeBytes,
            blockBytes,
            createBytes,
            createCount,
            isMounded
        )
    }

    override fun toString(): String {
        return """UsbInfo{
 devPath='$devPath', 
 serialNum='$serialNum', 
 productId=$productId, 
 vendorId=$vendorId, 
 totalBytes=$totalBytes, 
 freeBytes=$freeBytes, 
 blockBytes=$blockBytes, 
 createBytes=$createBytes, 
 createCount=$createCount, 
 isMounded=$isMounded}"""
    }

}
