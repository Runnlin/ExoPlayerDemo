package io.github.runnlin.exoplayerdemo

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.runnlin.exoplayerdemo.data.MediaInfo
import io.github.runnlin.exoplayerdemo.data.MediaRepository
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException
import android.content.Intent

import android.os.Environment

import android.os.Build
import android.provider.Settings
import com.google.android.material.timepicker.TimeFormat
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


private val TAG = "MainViewModel"

class MainViewModel(private val repository: MediaRepository) : ViewModel() {

    val usbMessPath = "/storage/usb0/"

    //    val usbMessPath = ScanFileUtil.externalStorageDirectory
//    val usbMessPath = "content://com.android.externalstorage.documents/document/0E6C-A005:"
    val internalPath = "/storage/self/primary/Movies/"
    val logFileName = "DecoderTestLog.txt"

    var isExternalStorage = false

    val allMediaInfo: LiveData<List<MediaInfo>> = repository.allFileInfo
    var currentPosition: Int = -1
    lateinit var currentMediaInfo: MediaInfo

    lateinit var current: LocalDateTime
    lateinit var formatter: DateTimeFormatter
    lateinit var formatted: String

    private lateinit var logFile: File

    fun initLogFile() {
        if (!isExternalStorage) return
        val fileName = usbMessPath + logFileName
        logFile = File(fileName)
        if (logFile.canWrite()) {
            logFile.printWriter().use { out ->
                out.println("\n\n------------${LocalDateTime.now()}------------\n")
            }
            Log.i(TAG, "initLogFile Success: $fileName")
        }
    }

    fun saveLog(content: String) {
        if (!logFile.exists() || !isExternalStorage || !logFile.canWrite()) return
        Log.i(TAG, "saveLog: $content")

        current = LocalDateTime.now()
        formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss.SSSS")
        formatted = current.format(formatter)

        logFile.printWriter().use { out ->
            out.println("$formatted-->$content")
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