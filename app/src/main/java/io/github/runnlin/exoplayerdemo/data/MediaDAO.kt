package io.github.runnlin.exoplayerdemo.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MediaDAO {

    @Query("SELECT * FROM MediaInfo ORDER BY uuid ASC")
    fun getAllFileInfo(): LiveData<List<MediaInfo>>

    @Query("SELECT * FROM MediaInfo WHERE TYPE LIKE :type ORDER BY uuid ASC")
    fun getSpecialFileInfo(type: String?): LiveData<List<MediaInfo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(media_info: MediaInfo)

    @Query("DELETE FROM MediaInfo")
    suspend fun deleteAllFileInfo()
}