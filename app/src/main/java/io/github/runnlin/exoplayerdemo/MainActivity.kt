package io.github.runnlin.exoplayerdemo

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
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
import androidx.core.view.size
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
import java.io.FileNotFoundException
import java.io.IOException


private const val TAG = "ZRL|ExoMainActivity"
private var DELAY_TIME: Long = 20L

@SuppressLint("SdCardPath")
class MainActivity : AppCompatActivity(), MediaListAdapter.onItemClickListener,
    SurfaceHolder.Callback {

    private lateinit var _recyclerView: RecyclerView
    private lateinit var _floatBtn: ExtendedFloatingActionButton
    private lateinit var _floatBtnLocal: ExtendedFloatingActionButton
    private lateinit var _editText: EditText
    private lateinit var _id3Info: TextView
    private lateinit var _playerView: SurfaceView
    private lateinit var _player: MediaPlayer

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var _swLoop: Switch
    private lateinit var _cover: ImageView

    private lateinit var _binding: ActivityMainBinding
    private lateinit var mediaListAdapter: MediaListAdapter
    private var builderForInfoDialog: CustomDialog.Builder? = null

    private lateinit var _scanFile: ScanFileUtil

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

        val holder = _playerView.holder
        holder.addCallback(this)
//        mainViewModel.initLogFile()
//        mainViewModel.deleteAll()
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

    private fun setPathAndScan(isUSB: Boolean) {
        if (_player.isPlaying) {
            _player.stop()
            _player.reset()
        }
        mainViewModel.deleteAll()
        initScan(isUSB)
        if (isUSB) {
//            mainViewModel.usbMessPath = Environment.getExternalStorageDirectory().toString()
//            rootPath = mainViewModel.usbMessPath
            _editText.setText(mainViewModel.usbMessPath)
        } else {
            _editText.setText(mainViewModel.internalPath)
            mainViewModel.isExternalStorage = false
//            getAllFilesInResources()
        }
        scan()
    }

    private fun initReceiver() {
        val usbReceiver = USBReceiver { usbDiskMountState, data ->
            Log.i(TAG, "USBReceiver state:$usbDiskMountState, data:$data")
            when (usbDiskMountState) {
                USBReceiver.USB_DISK_MOUNTED -> {
                    // 更新后不需要再通过广播获取u盘路径，可以直接
                    // Environment.getExternalStorageDirectory().toString()
                    // TODO text for older system
                    mainViewModel.usbMessPath = data
                    setPathAndScan(true)
//                    mainViewModel.initLogFile()
//                    scan()
                }
                USBReceiver.USB_DISK_UNMOUNTED -> {
                    setPathAndScan(false)
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
        _id3Info = _binding.id3Info
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
            Log.i(TAG, "_floatBtn load usb")
            setPathAndScan(true)
//            scan()
        }

        _floatBtnLocal.setOnClickListener {
            Log.i(TAG, "_floatBtnLocal ")
            setPathAndScan(false)
//            getAllFilesInResources()
//            scan()
        }

        _swLoop.setOnCheckedChangeListener { _, isChecked ->
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
        // clear surface view
        _playerView.holder.setFormat(PixelFormat.TRANSPARENT)
        _playerView.holder.setFormat(PixelFormat.OPAQUE)

        try {
            _player.setDataSource(this, path)
//            _player.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT)
            _player.prepareAsync()

        } catch (e: IllegalStateException) {
            Log.e(TAG, "!!!!!!!Play ERROR:${e.printStackTrace()}")
            myOnError(1)
        } catch (e: IOException) {
            Log.e(TAG, "!!!!!!!Play ERROR:${e.printStackTrace()}")
            myOnError(-1004)
        } catch (e: RuntimeException) {
            Log.e(TAG, "!!!!!!!Play ERROR:${e.printStackTrace()}")
            myOnError(1)
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

                myOnError(what, extra)
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

    private fun myOnError(what: Int = 0, extra: Int = 0) {
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
        // 当出现IO错误（USB读取失败）时，就没必要再继续尝试播放了
        if (what == 1) {
            isAutoPlay = false
            _swLoop.isChecked = false
            _player.reset()
        } else {
            delayPlayNextMedia()
        }

    }

    private fun initScan(isUsb: Boolean) {
        _scanFile = if (isUsb)
            ScanFileUtil(mainViewModel.usbMessPath)
        else
            ScanFileUtil(mainViewModel.internalPath)
        _scanFile.setCallBackFilter(
            ScanFileUtil.FileFilterBuilder().apply {
                onlyScanFile()
                scanMusicFiles()
                scanVideoFiles()
            }.build()
        )
        _scanFile.setScanFileListener(object : ScanFileUtil.ScanFileListener {
            override fun scanBegin() {
                Log.i(TAG, "Scan Begin: ${mainViewModel.usbMessPath}")
//                _player.clearMediaItems()
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
                    delay(_recyclerView.size * 100L)
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

    var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // There are no request codes
//            if (mainViewModel.initLogFile()) {
                startScan()
//            }
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
                if (Environment.isExternalStorageManager()) {
                    if (mainViewModel.initLogFile()) {
                        startScan()
                    }
                } else {
                    val intent = Intent()
                    intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
//                    intent.data = Uri.parse(packageName)
                    resultLauncher.launch(intent)
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
        Log.i(TAG, "Start Scan Path: ${mainViewModel.usbMessPath}")
        if (!File(mainViewModel.usbMessPath).exists()) {
            Log.e(TAG, "NO USB disk")
            Toast.makeText(this@MainActivity, "NO USB disk", Toast.LENGTH_SHORT)
                .show()
            return
        }
        _scanFile.stop()
        mainViewModel.isExternalStorage = true
        if (_player.isPlaying) {
            _player.stop()
            _player.reset()
        }
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
        if (_player.isPlaying)
            _player.stop()
        // 重置失败状态
        isFailed = false
        if (_recyclerView.size == 0) {
            _player.reset()
            return
        }
        if (mainViewModel.currentPosition >= mainViewModel.allMediaInfo.value?.size ?: -1) {
            if (isAutoPlay && !isFailed) {
                mainViewModel.currentPosition = 0
                playMedia()
            } else {
                _player.stop()
                _player.reset()
                mainViewModel.saveLog("本次测试结束\n\n")
            }
        } else {
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

//                if (mainViewModel.currentMediaInfo.path != null) {
                val mmr = MediaMetadataRetriever()
                try {
                    mmr.setDataSource(mainViewModel.currentMediaInfo.path)
                    val id3Info = "File Path:" + path +
                            "\n\nTITLE:   " + (mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                        ?: "NO TITLE") +
                            "\nALBUM:   " + (mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                        ?: "NO ALBUM") +
                            "\nArtist:   " + (mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                        ?: "NO ARTIST") +
                            "\nminiType:   " + (mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                        ?: "NO miniType") + ("\n\n\n")
                    mainViewModel.saveLog(id3Info)
                    _id3Info.text = id3Info
                } catch (e: FileNotFoundException) {
                    e.printStackTrace();
                } catch (e: IOException) {
                    e.printStackTrace();
                } catch (e: RuntimeException) {
                    e.printStackTrace();
                } finally {
                    mmr.release()
                }
//                }
            } else {
                Log.i(
                    TAG,
                    "play stop"
                )
                _player.release()
            }
        }
    }

    private fun setSurfaceDimensions(player: MediaPlayer, width: Int, height: Int) {
        if (width > 0 && height > 0) {
            val aspectRatio =
                width.toFloat() / height.toFloat()
            val surfaceHeight = _playerView.height
            val surfaceWidth = (surfaceHeight * aspectRatio).toInt()
            val params =
                RelativeLayout.LayoutParams(surfaceWidth, surfaceHeight)
            _playerView.layoutParams = params
            val holder = _playerView.holder
            player.setDisplay(holder)
        }
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