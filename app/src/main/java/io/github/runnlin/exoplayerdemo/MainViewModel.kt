package io.github.runnlin.exoplayerdemo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Environment
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.runnlin.exoplayerdemo.data.MediaInfo
import io.github.runnlin.exoplayerdemo.data.MediaRepository
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.HashSet


private const val TAG = "ZRL|MainViewModel"

class MainViewModel(private val repository: MediaRepository) : ViewModel() {

    var usbMessPath = "/storage/usb1"
//    val usbMessPath = "/mnt/media_rw/usb0/"

    //    val usbMessPath = ScanFileUtil.externalStorageDirectory
//    val usbMessPath = "content://com.android.externalstorage.documents/document/0E6C-A005:"
//    val internalPath: String = Environment.getExternalStorageDirectory().path
    val LOG_FILE_NAME = "DesaysvMediaTesterLog.txt"

    var isExternalStorage = false
    var isLogEnable = false
    private var filePath = ""

    val allMediaInfo: LiveData<List<MediaInfo>> = repository.allFileInfo
    var currentPosition: Int = -1
    var playErrorFileSet: HashSet<String> = HashSet()
    lateinit var currentMediaInfo: MediaInfo

    private lateinit var logFile: File

    fun initLogFile(): Boolean {
        Log.i(
            TAG,
            "Environment.getExternalStorageDirectory(): " + usbMessPath
        )
//        usbMessPath = Environment.getExternalStorageDirectory().path
        if (isLogEnable)
            return true
        else if (File(usbMessPath).isDirectory) {
            filePath = "$usbMessPath/$LOG_FILE_NAME"
            logFile = File(filePath)
            if (!logFile.exists() || !logFile.canWrite()) {
                try {
                    if (logFile.createNewFile()) {
                        Log.i(TAG, "createLogFile Success: $filePath")
                        initFirstLog()
                    }
                } catch (e: IOException) {
                    Log.i(TAG, "createLogFile Failed: $e")
                }
            } else {
                initFirstLog()
            }
        } else {
            Log.i(TAG, "Can't Create LogFile, no messPath ")
            isLogEnable = false
        }
        return isLogEnable
    }

    private fun initFirstLog() {
        try {
            Files.write(
                Paths.get(logFile.toURI()),
                "\n\n------------${LocalDateTime.now()}------------\n\n".toByteArray(),
                StandardOpenOption.APPEND
            )
            isLogEnable = true
        } catch (e: IOException) {
            Log.i(TAG, "Open logFile Failed: $e")
        }
    }

    private fun isVideo(type: String?): Boolean {
        when (type?.lowercase(Locale.getDefault())) {
            "mp4", "avi", "flv", "3gp", "mkv", "wmv", "m4v", "rmvb", "vob", "webm", "mpeg", "mpg", "mov", "ts" -> return true
        }
        return false
    }

    fun saveLog(content: String) {
        Log.i(TAG, "saveLog: $content")
        if (isExternalStorage && isLogEnable) {

            val current = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss.SSSS")
            val formatted = current.format(formatter)

            try {
                Files.write(
                    Paths.get(logFile.toURI()),
                    "$formatted-->$content\n".toByteArray(),
                    StandardOpenOption.APPEND
                )
            } catch (e: IOException) {
                Log.i(TAG, "saveLog Failed: $e")
            }
        }
    }

    fun insert(mediaInfo: MediaInfo) = viewModelScope.launch {
        Log.i(TAG, "insert: ${mediaInfo.path}")
        repository.insert(mediaInfo)
    }

    fun update(mediaInfo: MediaInfo) = viewModelScope.launch {
        Log.i(TAG, "update: ${mediaInfo.title}")
        repository.update(mediaInfo)
    }

    fun deleteAll() = viewModelScope.launch {
        Log.i(TAG, "deleteALL")
        repository.deleteAllFileInfo()
    }

    fun whatToString(isError: Boolean, what: Int): String {
        return if (isError) {
            when (what) {
                1 -> "MEDIA_ERROR_UNKNOWN"
                100 -> "MEDIA_ERROR_SERVER_DIED"
                200 -> "MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK"
                -1004 -> "MEDIA_ERROR_IO"
                -1007 -> "MEDIA_ERROR_MALFORMED"
                -1010 -> "MEDIA_ERROR_UNSUPPORTED"
                -110 -> "MEDIA_ERROR_TIMED_OUT"
                -2147483648 -> "MEDIA_ERROR_SYSTEM"
                -7788 -> "MEDIA_ERROR_GET_DURATION"
                else -> "UNKNOWN ERROR"
            }
        } else {
            when (what) {
                1 -> "MEDIA_INFO_UNKNOWN"
                2 -> "MEDIA_INFO_STARTED_AS_NEXT"
                3 -> "MEDIA_INFO_VIDEO_RENDERING_START"
                700 -> "MEDIA_INFO_VIDEO_TRACK_LAGGING"
                701 -> "MEDIA_INFO_BUFFERING_START"
                702 -> "MEDIA_INFO_BUFFERING_END"
                else -> {
                    "UNKNOWN INFO"
                }
            }
        }
    }

    fun getAlbumImage(mmr: MediaMetadataRetriever): Bitmap? {
        if (!isVideo(currentMediaInfo.type)) {
            try {
                val data = mmr.embeddedPicture
                return if (data != null) {
                    val bis = ByteArrayInputStream(data)
                    Log.i(TAG, "get mmr cover!$bis")
                    BitmapFactory.decodeStream(bis)
                } else null
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }
        return null
    }
}

class MediaViewModelFactory(private val repository: MediaRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}