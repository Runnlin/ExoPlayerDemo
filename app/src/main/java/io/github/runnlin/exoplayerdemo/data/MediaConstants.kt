package io.github.runnlin.exoplayerdemo.data

class MediaConstants {
    object FileLimit {
        const val MAX_FOLDER_LAYER = 8
        const val MAX_FOLDER_COUNT = 3000
        const val MAX_FILE_COUNT = 9999
        const val MAX_FILE_COUNT_IN_FOLDER = 255
        const val MAX_LIST_COUNT = 1000 //List<FileInfo>.size() >= 1000 --> insert DB
        const val MAX_VIDEO_FILE = 1024L * 1024L * 1024L * 2L //2G
        const val MAX_MUSIC_FAVORITE = 500
    }

    object DeviceType {
        const val DEVICE_LOCAL = "local"
        const val DEVICE_USB = "usb"
        const val DEVICE_USB_A = "usb_A"
        const val DEVICE_USB_B = "usb_B"
        const val DEVICE_SD_CARD = "sd_card"
        const val DEVICE_IPOD = "ipod"
        const val DEVICE_ALL = "all"
    }

    object DevicePath {
        const val PATH_LOCAL = "/storage/sdcard0"
        const val PATH_USB = "/storage/usb"
        const val PATH_USB_A = "/storage/usb0"
        const val PATH_USB_B = "/storage/usb1"
        const val PATH_SD_CARD = "/storage/sdcard1"
        const val PATH_IPOD = "/storage/sdcard1"
    }

    object DeviceStatus {
        const val STATUS_UNKNOWN = "/unknown"
        const val STATUS_EJECT = "/eject"
        const val STATUS_MOUNTED = "/mounted"
        const val STATUS_INDEXING = "/indexing"
        const val STATUS_INDEXED = "/indexed"
        const val STATUS_PARSING = "/parsing"
        const val STATUS_PARSED = "/parsed"
        const val STATUS_NO_MUSIC = ".no.music"
        const val STATUS_NO_VIDEO = ".no.video"
        const val STATUS_NO_IMAGE = ".no.image"
        const val STATUS_GOT_MUSIC = ".got.music"
        const val STATUS_GOT_VIDEO = ".got.video"
        const val STATUS_GOT_IMAGE = ".got.image"
    }

    object MediaType {
        const val TYPE_UNKNOWN = 0x00
        const val TYPE_MUSIC = 0x01
        const val TYPE_VIDEO = 0x02
        const val TYPE_IMAGE = 0x04
        const val TYPE_MEDIA = TYPE_MUSIC or TYPE_VIDEO or TYPE_IMAGE //7
        const val TYPE_MUSIC_VIDEO = TYPE_MUSIC or TYPE_VIDEO //3
        const val TYPE_DIRECTORY = 0x08
    }

    object DataKey {
        const val KEY_UNKNOWN = 0x20
        const val KEY_MEDIA = 0x21
        const val KEY_MUSIC = 0x22
        const val KEY_VIDEO = 0x23
        const val KEY_IMAGE = 0x24
        const val KEY_FOLDER = 0x25
        const val KEY_FILE = 0x26
        const val KEY_FILE_IN_FOLDER = 0x27
    }
}
