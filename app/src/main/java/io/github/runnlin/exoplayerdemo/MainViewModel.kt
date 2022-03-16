package io.github.runnlin.exoplayerdemo

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.runnlin.exoplayerdemo.data.MediaInfo
import io.github.runnlin.exoplayerdemo.data.MediaRepository
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*


private val TAG = "ZRL|MainViewModel"

class MainViewModel(private val repository: MediaRepository) : ViewModel() {

    val usbMessPath = "/storage/usb0/"
//    val usbMessPath = "/mnt/media_rw/usb0/"

    //    val usbMessPath = ScanFileUtil.externalStorageDirectory
//    val usbMessPath = "content://com.android.externalstorage.documents/document/0E6C-A005:"
    val internalPath = "/storage/self/primary/Movies/"
    val logFileName = "DesaysvDecodeTesterLog.txt"

    var isExternalStorage = false
    var isLogEnable = false
    private var filePath = ""
    private lateinit var fileOutputStream: FileOutputStream

    val allMediaInfo: LiveData<List<MediaInfo>> = repository.allFileInfo
    var currentPosition: Int = -1
    lateinit var currentMediaInfo: MediaInfo

    private lateinit var logFile: File

    fun initLogFile(): Boolean {
        Log.i(TAG, "Ready createLogFile ")

        if (File(usbMessPath).exists()) {
            filePath = usbMessPath + logFileName
            fileOutputStream = FileOutputStream(filePath)
            logFile = File(filePath)
            if (!logFile.exists()) {
                logFile.createNewFile()
                Log.i(TAG, "createLogFile Success: $filePath")
            }
            if (logFile.canWrite()) {
                try {
                    Files.write(
                        Paths.get(logFile.toURI()),
                        "\n\n------------${LocalDateTime.now()}------------\n\n".toByteArray(),
                        StandardOpenOption.APPEND
                    )
                    isLogEnable = true
                    return true
                } catch (e: IOException) {
                    Log.i(TAG, "initLogFile Failed: $e")
                }
            }
        } else {
            Log.i(TAG, "Can't Create LogFile, no messPath ")
            isLogEnable = false
        }
        return false
    }

    fun isVideo(type: String?): Boolean {
        when (type?.lowercase(Locale.getDefault())) {
            "mp4", "avi", "flv", "3gp", "mkv", "wmv", "m4v", "rmvb", "vob", "webm", "mpeg", "mpg", "mov" -> return true
        }
        return false
    }

    fun saveLog(content: String) {
        if (!isExternalStorage && !isLogEnable) return
        Log.i(TAG, "saveLog: $content")

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
}

class MediaViewModelFactory(private val repository: MediaRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknow ViewModel class")
    }
}