package io.github.runnlin.exoplayerdemo

import android.app.Application
import io.github.runnlin.exoplayerdemo.data.MediaRepository
import io.github.runnlin.exoplayerdemo.data.MediaRoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class ExpPlayerDemoApplication: Application() {

    override fun onCreate() {
        super.onCreate()

//        UEventReceiver.getInstance().init(this)
    }

    override fun onTerminate() {
        super.onTerminate()

//        UEventReceiver.getInstance().destroy()
    }
    private val applicationScope = CoroutineScope(SupervisorJob())

    private val database by lazy {
        MediaRoomDatabase.getDatabase(this, applicationScope)
    }

    val repository by lazy {
        MediaRepository(database.mediaDAO())
    }
}