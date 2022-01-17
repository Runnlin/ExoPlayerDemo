package io.github.runnlin.exoplayerdemo

import android.Manifest
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import io.github.runnlin.exoplayerdemo.data.MediaInfo
import io.github.runnlin.exoplayerdemo.databinding.ActivityMainBinding
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.*


private const val TAG = "ExoMainActivity"
private var DELAY_TIME: Long = 3L
private var rootPath = "/storage/self/primary/Movies"

class MainActivity : AppCompatActivity(), MediaListAdapter.onItemClickListener, Player.Listener {

    private lateinit var _recyclerView: RecyclerView
    private lateinit var _floatBtn: ExtendedFloatingActionButton
    private lateinit var _playerView: PlayerView
    private lateinit var _player: ExoPlayer
    private lateinit var _binding: ActivityMainBinding
    private lateinit var mediaListAdapter: MediaListAdapter
    private var builderForInfoDialog: CustomDialog.Builder? = null

    //    private var _infoDialog: CustomDialog? = null
    private lateinit var _scanFile: ScanFileUtil

    private var isAutoPlay = false

    private val mainViewModel: MainViewModel by viewModels {
        MediaViewModelFactory((application as ExpPlayerDemoApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(_binding.root)

        initReceiver()
        initView()
        mainViewModel.initLogFile()
    }

    override fun onResume() {
        super.onResume()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, _binding.root).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun initReceiver() {

        val usbReceiver = USBReceiver { usbDiskMountState ->
            Log.i(TAG, "USB: ${usbDiskMountState}")
            when (usbDiskMountState) {
                USBReceiver.USB_DISK_MOUNTED -> {
                    rootPath = mainViewModel.usbMessPath
                    mainViewModel.isExternalStorage = true
                    scan()
                }
                USBReceiver.USB_DISK_UNMOUNTED -> {
                    rootPath = mainViewModel.internalPath
                    mainViewModel.isExternalStorage = false
                    scan()
                }
            }
        }
        val usbDeviceStateFilter = IntentFilter()
        usbDeviceStateFilter.addAction(Intent.ACTION_MEDIA_MOUNTED)
        usbDeviceStateFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED)
        usbDeviceStateFilter.addDataScheme("file")
        registerReceiver(usbReceiver, usbDeviceStateFilter)
    }

    private fun initView() {
        _recyclerView = _binding.rvPlaylist
        _playerView = _binding.videoView
        _floatBtn = _binding.floatBtn
        builderForInfoDialog = CustomDialog.Builder(this)
        mediaListAdapter = MediaListAdapter()
        mediaListAdapter.addItemClickListener(this)
        _recyclerView.apply {
            adapter = mediaListAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
            addItemDecoration(
                DividerItemDecoration(
                    this@MainActivity,
                    DividerItemDecoration.VERTICAL
                )
            )
        }
        mainViewModel.allMediaInfo.observe(this, { medias ->
            mediaListAdapter.submitList(medias)
        })

        _player = ExoPlayer.Builder(this).build().apply {
            playWhenReady = true
            pauseAtEndOfMediaItems = true
            _playerView.player = this
            addListener(this@MainActivity)
        }

        _floatBtn.setOnClickListener {
            Log.i(TAG, "setOnClickListener")
            scan()
        }
    }

    private fun initScan() {
        _scanFile = ScanFileUtil(rootPath)
        _scanFile.setCallBackFilter(
            ScanFileUtil.FileFilterBuilder().apply {
                scanVideoFiles()
                scanMusicFiles()
            }.build()
        )
        _scanFile.setScanFileListener(object : ScanFileUtil.ScanFileListener {
            override fun scanBegin() {
                Log.i(TAG, "Scan Begin: $rootPath")
                _player.clearMediaItems()
            }

            override fun scanComplete(timeConsuming: Long) {
                Log.i(TAG, "Scan Done, files: ${mainViewModel.allMediaInfo.value}")
                Toast.makeText(
                    this@MainActivity,
                    "Scan Done, consumed: $timeConsuming",
                    Toast.LENGTH_SHORT
                ).show()
                isAutoPlay = true
                mainViewModel.currentPosition = 0

                MainScope().launch {
                    delay(2000)
                    playMedia()
                }
            }

            override fun scanningCallBack(file: File) {
                mainViewModel.insert(packageMediaFile(file))
            }
        })
    }

    // Player Listener
    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        Log.i(TAG, "Player ERROR: ${error.errorCodeName},   ${error.message}")
        mainViewModel.currentMediaInfo.isAbility = 2
        mediaListAdapter.notifyItemChanged(mainViewModel.currentPosition)
        mainViewModel.saveLog("播放失败: ${error.errorCodeName},   ${error.message}\n\n")
        if (isAutoPlay) {
            MainScope().launch {
                delay(500)
                playNextMedia()
            }
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        super.onPlaybackStateChanged(playbackState)
        when (playbackState) {
            Player.STATE_ENDED -> {
                if (!isAutoPlay && mainViewModel.currentMediaInfo.isAbility != 2) {
                    mainViewModel.currentMediaInfo.isAbility = 1
                    mediaListAdapter.notifyItemChanged(mainViewModel.currentPosition)
                }
            }
            Player.STATE_READY -> {
                if (!isAutoPlay) {
                    mainViewModel.currentMediaInfo.isAbility = 1
                    mediaListAdapter.notifyItemChanged(mainViewModel.currentPosition)
                }
            }
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        Log.i(
            TAG,
            "onIsPlayingChanged, isPlaying: $isPlaying"
        )
        super.onIsPlayingChanged(isPlaying)
        //  *首次* isPlaying为true时说明正常播放
        if (isPlaying) {
            Log.i(
                TAG,
                "onIsPlayingChanged, position: ${mainViewModel.currentPosition}"
            )
            if (isAutoPlay) {
                DELAY_TIME = if (_player.duration < DELAY_TIME) _player.duration else DELAY_TIME
                object : CountDownTimer(DELAY_TIME * 1000L, 100L) {
                    override fun onTick(millisUntilFinished: Long) {
                        if (mainViewModel.currentMediaInfo.isAbility == 2)
                            this.cancel()
                    }

                    override fun onFinish() {
                        mainViewModel.currentMediaInfo.isAbility = 1
                        mediaListAdapter.notifyItemChanged(mainViewModel.currentPosition)
                        mainViewModel.saveLog("播放成功\n\n")
                        playNextMedia()
                    }
                }.start()
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                startScan()
            } else {
                Toast.makeText(this@MainActivity, "NO Permission, NO Scan", Toast.LENGTH_SHORT)
                    .show()
            }
        }

    private fun scan() {
        Log.i(TAG, "Scan Path: $rootPath")
        mainViewModel.initLogFile()
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (Environment.isExternalStorageManager()) {
                        startScan()
                    } else {
                        val intent = Intent()
                        intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                        startActivity(intent)
                    }
                } else {
                    startScan()
                }
            }
            else -> {
                requestPermissionLauncher.launch(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            }
        }
    }

    private fun startScan() {
        if (_player.isPlaying) {
            _player.stop()
        }
        mainViewModel.deleteAll()
        initScan()
        _scanFile.startAsyncScan()
    }

    fun packageMediaFile(file: File): MediaInfo {
        return MediaInfo(
            file.name,
            file.extension,
            file.length().toInt(),
            file.name,
            file.length().toInt(),
            file.canonicalPath
        )
    }

    override fun onPlayListener(mediaInfo: MediaInfo, position: Int) {
        mainViewModel.currentMediaInfo = mediaInfo
        mainViewModel.currentPosition = position
        isAutoPlay = false
        playMedia()
    }

    private fun playNextMedia() {
        mainViewModel.currentPosition++
        playMedia()
    }

    private fun playMedia() {
//        if (_player.isPlaying)
//            _player.stop()
        if (mainViewModel.currentPosition >= mainViewModel.allMediaInfo.value?.size ?: -1) {
            _recyclerView.smoothScrollToPosition(0)
        } else if (mainViewModel.allMediaInfo.value?.get(mainViewModel.currentPosition) != null) {
            mainViewModel.currentMediaInfo =
                mainViewModel.allMediaInfo.value!![mainViewModel.currentPosition]
            _recyclerView.scrollToPosition(mainViewModel.currentPosition)

            if (!mainViewModel.currentMediaInfo.path.isNullOrEmpty()) {

                val path =
                    mainViewModel.currentMediaInfo.path ?: "https://v-cdn.zjol.com.cn/276982.mp4"
                Log.i(
                    TAG,
                    "start_play: ${mainViewModel.currentPosition}, path: ${path}"
                )
                val mediaItem = MediaItem.fromUri(
                    path
                )
                _player.setMediaItem(mediaItem)
                _player.prepare()
//                _player.play()
                mainViewModel.currentMediaInfo.isAbility = 3
                mediaListAdapter.notifyItemChanged(mainViewModel.currentPosition)

                try {
                    val mmr = MediaMetadataRetriever()
                    mmr.setDataSource(mainViewModel.currentMediaInfo.path)
                    Log.i(
                        TAG,
                        "TITLE:" + (mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                            ?: "NO TITLE") +
                                "\nALBUM:" + (mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                            ?: "NO ALBUM")
                    )
                    mainViewModel.saveLog(
                        "TITLE:" + (mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                            ?: "NO TITLE") +
                                "   ALBUM:" + (mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                            ?: "NO ALBUM") + "  miniType:" + (mmr.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_MIMETYPE
                        ))
                    )
                } catch (e: RuntimeException) {
                    Log.e(TAG, e.stackTraceToString())
                }

            }

        }
    }

}