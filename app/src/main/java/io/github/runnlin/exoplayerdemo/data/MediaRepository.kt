package io.github.runnlin.exoplayerdemo.data

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData

class MediaRepository(private val mediaDAO: MediaDAO) {
    val allFileInfo: LiveData<List<MediaInfo>> = mediaDAO.getAllFileInfo()

    @WorkerThread
    suspend fun insert(mediaInfo: MediaInfo) {
        mediaDAO.insertFile(mediaInfo)
    }

    @WorkerThread
    suspend fun update(mediaInfo: MediaInfo) {
        mediaDAO.updateFile(mediaInfo)
    }

    @WorkerThread
    suspend fun deleteAllFileInfo() {
        mediaDAO.deleteAllFileInfo()
//        insert(
//            MediaInfo(
//                uuid = "1",
//                type = "mp4",
//                title = "Network Media Test",
//                path = "https://v-cdn.zjol.com.cn/277010.mp4"
//            )
//        )
    }

}