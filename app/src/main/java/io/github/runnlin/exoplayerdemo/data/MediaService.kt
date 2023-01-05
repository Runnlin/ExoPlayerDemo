package io.github.runnlin.exoplayerdemo.data

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import io.github.runnlin.exoplayerdemo.MainViewModel

class MediaService: Service() {

    private var mainViewModel: MainViewModel? = null

    public fun setMainViewModel(mainViewModel: MainViewModel) {
        this.mainViewModel = mainViewModel
    }

    override fun onBind(p0: Intent?): IBinder {

        return myBinder
    }

    override fun onDestroy() {
        super.onDestroy()
        mainViewModel = null
    }

    class MyBinder : Binder() {
        val myService: MediaService
            get() = MediaService()
    }

    private val myBinder = MyBinder()

}