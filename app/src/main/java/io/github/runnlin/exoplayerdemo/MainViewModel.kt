package io.github.runnlin.exoplayerdemo

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.runnlin.exoplayerdemo.data.MediaInfo
import io.github.runnlin.exoplayerdemo.data.MediaRepository
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException


private val TAG = "MainViewModel"
class MainViewModel(private val repository: MediaRepository): ViewModel() {


    val allMediaInfo: LiveData<List<MediaInfo>> = repository.allFileInfo
    var currentPosition: Int = -1
    lateinit var currentMediaInfo: MediaInfo

    fun insert(mediaInfo: MediaInfo) = viewModelScope.launch {
        Log.i(TAG, "insert: ${mediaInfo.title}")
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

class MediaViewModelFactory(private val repository: MediaRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknow ViewModel class")
    }
}