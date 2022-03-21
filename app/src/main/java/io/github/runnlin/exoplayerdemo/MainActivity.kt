package io.github.runnlin.exoplayerdemo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.*
import android.media.MediaPlayer.OnCompletionListener
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import io.github.runnlin.exoplayerdemo.data.MediaInfo
import io.github.runnlin.exoplayerdemo.databinding.ActivityMainBinding
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File


private const val TAG = "ZRL|ExoMainActivity"
private var DELAY_TIME: Long = 20L

@SuppressLint("SdCardPath")
//private var rootPath = "/sdcard/Movies"
//private var rootPath = "/mnt/media_rw/usb0/"
private var rootPath = "/storage/usb0"

class MainActivity : AppCompatActivity(), MediaListAdapter.onItemClickListener,
    SurfaceHolder.Callback {

    private lateinit var _recyclerView: RecyclerView
    private lateinit var _floatBtn: ExtendedFloatingActionButton
    private lateinit var _floatBtnLocal: ExtendedFloatingActionButton
    private lateinit var _editText: EditText
    private lateinit var _playerView: SurfaceView
    private lateinit var _player: MediaPlayer

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var _swLoop: Switch
    private lateinit var _cover: ImageView

    //    private lateinit var _player: ExoPlayer
    private lateinit var _binding: ActivityMainBinding
    private lateinit var mediaListAdapter: MediaListAdapter
    private var builderForInfoDialog: CustomDialog.Builder? = null

    //    private var _infoDialog: CustomDialog? = null
    private lateinit var _scanFile: ScanFileUtil
    private var playbackPosition = 0

    private var isAutoPlay = false // 自动切曲
    private var isFailed = false // 已经失败（onError回调会调用多次，返回不同的Extra）

    private val mainViewModel: MainViewModel by viewModels {
        MediaViewModelFactory((application as ExpPlayerDemoApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(_binding.root)

        initReceiver()
        initView()
        initScan()

        val holder = _playerView.holder
        holder.addCallback(this)
        mainViewModel.initLogFile()
    }

    override fun onStop() {
        _player.stop()
        _player.release()
        super.onStop()
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

    override fun onPause() {
        super.onPause()
        playbackPosition = _player.currentPosition
    }

    private fun initReceiver() {

        val usbReceiver = USBReceiver { usbDiskMountState ->
            Log.i(TAG, "USB: ${usbDiskMountState}")
            when (usbDiskMountState) {
                USBReceiver.USB_DISK_MOUNTED -> {
                    rootPath = _editText.text.toString()
                    scan()
                }
                USBReceiver.USB_DISK_UNMOUNTED -> {
                    mainViewModel.deleteAll()
                    mainViewModel.isExternalStorage = false
                    if (_player.isPlaying) {
                        _player.stop()
                        _player.release()
                    }
                }
            }
        }
        val usbDeviceStateFilter = IntentFilter()
        usbDeviceStateFilter.addAction(Intent.ACTION_MEDIA_MOUNTED)
        usbDeviceStateFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED)
        usbDeviceStateFilter.addDataScheme("file")
        registerReceiver(usbReceiver, usbDeviceStateFilter)
    }

    private fun getAllFilesInResources() {
        mainViewModel.deleteAll()
        for (i in 1..10) {
            val resourceID = resources.getIdentifier("a${i}", "raw", packageName)
            if (resourceID != 0) {
                var resourceName = resources.getResourceEntryName(resourceID)
                resourceName = resourceName.substring(
                    resourceName.lastIndexOf(
                        "/"
                    ) + 1
                )
                Log.i(TAG, resourceName)
                mainViewModel.insert(
                    MediaInfo(
                        uuid = "$resourceID",
                        title = resourceName,
                        type =
                        resourceName.substring(
                            resourceName.lastIndexOf(
                                "."
                            ) + 1
                        ),
                        path = "android.resource://$packageName/${resourceID}"
                    )
                )
            }
        }
    }

    private fun initView() {
        _recyclerView = _binding.rvPlaylist
        _playerView = _binding.videoView
        _floatBtn = _binding.floatBtn
        _floatBtnLocal = _binding.floatBtnLocal
        _editText = _binding.textInputEditText
        _swLoop = _binding.swLoop
        _cover = _binding.ivCover
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
        mainViewModel.allMediaInfo.observe(this) { medias ->
            mediaListAdapter.submitList(medias)
        }

        _floatBtn.setOnClickListener {
            Log.i(TAG, "_floatBtn path:$rootPath")
            rootPath = _editText.text.toString()
            scan()
        }

        _floatBtnLocal.setOnClickListener {
            Log.i(TAG, "_floatBtnLocal ")
            mainViewModel.isExternalStorage = false
            getAllFilesInResources()
        }

        _swLoop.setOnCheckedChangeListener { buttonView, isChecked ->
            isAutoPlay = isChecked
        }
    }

    private fun setupMediaPlayer(surface: Surface) {
        _player = MediaPlayer().apply {
            setSurface(surface)
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
        }
    }

    private fun prepareMediaPlayer(path: Uri) {
        _player.reset()
        try {
            _player.setDataSource(this, path)
//            _player.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT)
            _player.prepareAsync()

        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }

        /** PLAYER STATUS **/
        _player.setOnPreparedListener {
            _player.start()
            if (_player.isPlaying) {

                mainViewModel.currentMediaInfo.isAbility = 3
                mediaListAdapter.notifyItemChanged(mainViewModel.currentPosition)

                Log.i(TAG, "!!!!!!!isPlaying:\n${it.metrics}")
                if (isAutoPlay) {
                    val delayTime =
                        if (_player.duration < DELAY_TIME) _player.duration.toLong() else DELAY_TIME
                    object : CountDownTimer(delayTime * 1000L, 100) {
                        override fun onTick(millisUntilFinished: Long) {
                            if (mainViewModel.currentMediaInfo.isAbility == 2 ||
                                !isAutoPlay ||
                                !_player.isPlaying
                            )
                                this.cancel()
                        }

                        override fun onFinish() {
                            mainViewModel.currentMediaInfo.isAbility = 1
                            mediaListAdapter.notifyItemChanged(mainViewModel.currentPosition)
                            mainViewModel.saveLog("播放成功，自动切曲\n\n")
                            Log.i(TAG, "播放成功，自动切曲")
                            delayPlayNextMedia()
                        }
                    }.start()
                }
            }
        }

        _player.setOnVideoSizeChangedListener { player, width, height ->
            setSurfaceDimensions(player, width, height)
        }

        _player.setOnBufferingUpdateListener { mp, percent ->
            Log.i(TAG, "!!!!!!!isBuffering")
        }

        _player.setOnErrorListener { _, what, extra ->
            if (!isFailed) {
                // 确保只调用一次
                isFailed = true

                Log.wtf(
                    TAG,
                    "!!!!!!!ERROR: what:${mainViewModel.whatToString(true, what)} extra:$extra"
                )

                mainViewModel.currentMediaInfo.isAbility = 2
                mediaListAdapter.notifyItemChanged(mainViewModel.currentPosition)
                mainViewModel.saveLog(
                    "播放失败: what:${
                        mainViewModel.whatToString(
                            true,
                            what
                        )
                    },   extra:${extra}\n\n"
                )
                delayPlayNextMedia()
            }
            false

        }

        _player.setOnCompletionListener {
            if (!isFailed) {
                mainViewModel.currentMediaInfo.isAbility = 1
                mediaListAdapter.notifyItemChanged(mainViewModel.currentPosition)
                Log.i(TAG, "播放完成\n\n")
                mainViewModel.saveLog("播放完成\n\n")
                delayPlayNextMedia()
            }
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
//                _player.clearMediaItems()
                mainViewModel.deleteAll()
                _swLoop.isChecked = false
                _floatBtn.isEnabled = false
            }

            override fun scanComplete(timeConsuming: Long) {
//                Log.i(TAG, "Scan Done, files: ${mainViewModel.allMediaInfo.value}")
//                Toast.makeText(
//                    this@MainActivity,
//                    "Scan Done, consumed: $timeConsuming",
//                    Toast.LENGTH_SHORT
//                ).show()
                _swLoop.isChecked = true
                mainViewModel.currentPosition = 0

                MainScope().launch {
                    delay(3000)
                    playMedia()
                    _floatBtn.isEnabled = true
                }
            }

            override fun scanningCallBack(file: File) {
                mainViewModel.insert(packageMediaFile(file))
            }
        })
    }

    fun packageMediaFile(file: File): MediaInfo {
        return MediaInfo(
            file.name,
            file.extension,
            file.length().toInt(),
            file.name,
            file.length().toInt(),
            file.absolutePath
        )
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _isGranted ->
            if (_isGranted) {
                scan()
            } else {
                Toast.makeText(this@MainActivity, "NO Permission, NO Scan", Toast.LENGTH_SHORT)
                    .show()
            }
        }

    private fun scan() {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                    !Environment.isExternalStorageManager()
                ) {
                    val intent = Intent()
                    intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                    startActivity(intent)
                } else {
                    if (mainViewModel.initLogFile()) {
                        startScan()
                    }
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
        mainViewModel.isExternalStorage = true
        if (_player.isPlaying) {
            _player.stop()
        }
        _scanFile.stop()
        Log.i(TAG, "Start Scan Path: $rootPath")
        _scanFile.startAsyncScan()
    }

    /* 点击项目播放 */
    override fun onPlayListener(mediaInfo: MediaInfo, position: Int) {
        if (mainViewModel.currentPosition != -1 && mainViewModel.currentMediaInfo.isAbility == 3) {
            mainViewModel.currentMediaInfo.isAbility = 0
            mediaListAdapter.notifyItemChanged(mainViewModel.currentPosition)
        }
        mainViewModel.currentMediaInfo = mediaInfo
        mainViewModel.currentPosition = position
//        isAutoPlay = false

        playMedia()
    }

    private fun delayPlayNextMedia() {
        if (!isAutoPlay) return
        mainViewModel.currentPosition++
        MainScope().launch {
            delay(500)
            playMedia()
        }
    }

    private fun playMedia() {
//        if (_player.isPlaying)
//            _player.stop()
        // 重置失败状态
        isFailed = false

        if (mainViewModel.currentPosition >= mainViewModel.allMediaInfo.value?.size ?: -1) {
            _recyclerView.smoothScrollToPosition(0)
            mainViewModel.saveLog("本次测试结束\n\n")

            if (isAutoPlay) {
                mainViewModel.currentPosition = 0
                playMedia()
            }
        } else if (mainViewModel.allMediaInfo.value?.get(mainViewModel.currentPosition) != null) {
            mainViewModel.currentMediaInfo =
                mainViewModel.allMediaInfo.value!![mainViewModel.currentPosition]
            _recyclerView.smoothScrollToPosition(mainViewModel.currentPosition)

            if (!mainViewModel.currentMediaInfo.path.isNullOrEmpty()) {

                val path = mainViewModel.currentMediaInfo.path!!
                Log.i(
                    TAG,
                    "start_play: ${mainViewModel.currentPosition}, path: $path"
                )
                prepareMediaPlayer(Uri.parse(path))

                val cover = mainViewModel.getAlbumImage(path)

                if (null != cover) {
                    _playerView.visibility = View.INVISIBLE
                    _cover.visibility = View.VISIBLE
                    _cover.setImageBitmap(cover)
                } else {
                    _playerView.visibility = View.VISIBLE
                    _cover.visibility = View.INVISIBLE
                }

                if (mainViewModel.isExternalStorage)
                    try {
                        val mmr = MediaMetadataRetriever()
                        mmr.setDataSource(mainViewModel.currentMediaInfo.path)
                        mainViewModel.saveLog(
                            "File Path:" + path +
                                    "\nTITLE:" + (mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                                ?: "NO TITLE") +
                                    "   ALBUM:" + (mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                                ?: "NO ALBUM") + "  miniType:" + (mmr.extractMetadata(
                                MediaMetadataRetriever.METADATA_KEY_MIMETYPE
                            ))
                        )
                    } catch (e: RuntimeException) {
                        Log.e(TAG, e.stackTraceToString())
                    }
            } else {
                Log.i(
                    TAG,
                    "play stop"
                )
            }
        }
    }

    private fun setSurfaceDimensions(player: MediaPlayer, width: Int, height: Int) {
//        if (width > 0 && height > 0) {
        val aspectRatio =
            width.toFloat() / height.toFloat()
        val surfaceHeight = _playerView.height
        val surfaceWidth = (surfaceHeight * aspectRatio).toInt()
        val params =
            RelativeLayout.LayoutParams(surfaceWidth, surfaceHeight)
        _playerView.layoutParams = params
        val holder = _playerView.holder
        player.setDisplay(holder)
//        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        val surface = holder.surface
        setupMediaPlayer(surface)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
    }

    override fun onDestroy() {
        super.onDestroy()
        if (_player.isPlaying)
            _player.stop()
        _player.release()
    }
}