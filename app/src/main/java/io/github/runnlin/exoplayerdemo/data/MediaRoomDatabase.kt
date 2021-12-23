package io.github.runnlin.exoplayerdemo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Database(entities = [MediaInfo::class], version = 3, exportSchema = false)
abstract class MediaRoomDatabase : RoomDatabase() {
    abstract fun mediaDAO(): MediaDAO

    companion object {
        // 任何一个线程对它进行修改，都会让所有其他CPU高速缓存中的值过期，这样其他线程就必须去内存中重新获取最新的值
        @Volatile
        private var INSTANCE: MediaRoomDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): MediaRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MediaRoomDatabase::class.java,
                    "media_database"
                ).fallbackToDestructiveMigration().addCallback(MediaDatabaseCallback(scope)).build()
                INSTANCE = instance
                instance
            }
        }

    }

    private class MediaDatabaseCallback(private val scope: CoroutineScope) :
        RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch {
                    // 刚创建时进行清空初始化
                    database.mediaDAO().deleteAllFileInfo()
                    
                }
            }
        }
    }
}