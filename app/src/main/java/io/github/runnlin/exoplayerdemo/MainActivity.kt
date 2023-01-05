package io.github.runnlin.exoplayerdemo

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.*
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
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.switchmaterial.SwitchMaterial
import io.github.runnlin.exoplayerdemo.data.MediaInfo
import io.github.runnlin.exoplayerdemo.data.MediaService
import io.github.runnlin.exoplayerdemo.databinding.ActivityMainBinding
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.charset.Charset


private const val TAG = "ZRL|ExoMainActivity"
var DELAY_TIME: Long = 20 * 1000L

@SuppressLint("SdCardPath")
class MainActivity : AppCompatActivity(), MediaListAdapter.onItemClickListener,
    SurfaceHolder.Callback {

    private lateinit var _recyclerView: RecyclerView
    private lateinit var _floatBtnLocal: ExtendedFloatingActionButton
    private lateinit var _editText: EditText
    private lateinit var _progress: LinearProgressIndicator
    private lateinit var _id3Info: TextView
    private lateinit var _playerView: SurfaceView
    private lateinit var _player: MediaPlayer
    private lateinit var _autoTime: Spinner
    private lateinit var _infinityPlay: CheckBox

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var _swLoop: SwitchMaterial
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
        //应用运行时，保持屏幕高亮，不锁屏
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

//        mainViewModel.bindMediaService()
    }

    override fun onStop() {
        if (_player.isPlaying) {
            _player.stop()
        }
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

    private fun setPathAndScan() {
        mainViewModel.isExternalStorage = File(mainViewModel.usbMessPath).isDirectory
        if (null != _player && _player.isPlaying) {
            _player.stop()
//            _player.reset()
        }
        Toast.makeText(
            this,
            "start scan...",
            Toast.LENGTH_SHORT
        ).show()
        mainViewModel.deleteAll()
        initScan(mainViewModel.isExternalStorage)
        scan()
    }

    private fun initReceiver() {
        val usbReceiver = USBReceiver { usbDiskMountState, data ->
            Log.i(TAG, "USBReceiver state:$usbDiskMountState, data:$data")
            when (usbDiskMountState) {
                USBReceiver.USB_DISK_MOUNTED -> {
                    // 更新后不需要再通过广播获取u盘路径，可以直接
                    // Environment.getExternalStorageDirectory().toString()
                    Toast.makeText(
                        this,
                        "Received UDisk mounted, start scan $data",
                        Toast.LENGTH_SHORT
                    ).show()
                    if (data.contains("usb")) {
//                        mainViewModel.usbMessPath = data
                        if (File("/storage/usb1").isDirectory)
                            mainViewModel.usbMessPath = "/storage/usb1"
                        else if (File("/storage/usb0").isDirectory)
                            mainViewModel.usbMessPath = "/storage/usb0"
                        setPathAndScan()
                    }
//                    mainViewModel.initLogFile()
//                    scan()
                }
                USBReceiver.USB_DISK_UNMOUNTED -> {
                    if (_player.isPlaying) {
                        _player.stop()
                        _player.release()
                    }
//                    setPathAndScan(false)
                }
            }
        }
        val usbDeviceStateFilter = IntentFilter()
        usbDeviceStateFilter.addAction(Intent.ACTION_MEDIA_MOUNTED)
        usbDeviceStateFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED)
        usbDeviceStateFilter.addDataScheme("file")
        registerReceiver(usbReceiver, usbDeviceStateFilter)
    }

//    private fun getAllFilesInResources() {
//        for (i in 1..10) {
//            val resourceID = resources.getIdentifier("a${i}", "raw", packageName)
//            if (resourceID != 0) {
//                var resourceName = resources.getResourceEntryName(resourceID)
//                resourceName = resourceName.substring(
//                    resourceName.lastIndexOf(
//                        "/"
//                    ) + 1
//                )
//                Log.i(TAG, resourceName)
//                mainViewModel.insert(
//                    MediaInfo(
//                        uuid = "$resourceID",
//                        title = resourceName,
//                        type =
//                        resourceName.substring(
//                            resourceName.lastIndexOf(
//                                "."
//                            ) + 1
//                        ),
//                        path = "android.resource://$packageName/${resourceID}"
//                    )
//                )
//            }
//        }
//    }

    private fun initView() {
        _recyclerView = _binding.rvPlaylist
        _playerView = _binding.videoView
//        _floatBtn = _binding.floatBtn
        _floatBtnLocal = _binding.floatBtnLocal
        _progress = _binding.progress
        _editText = _binding.textInputEditText
        _id3Info = _binding.id3Info
        _swLoop = _binding.swLoop
        _cover = _binding.ivCover
        _autoTime = _binding.spinAutoplayTime
        _infinityPlay = _binding.cbInfinity
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

//        _floatBtn.setOnClickListener {
//            Log.i(TAG, "_floatBtn load usb")
//            setPathAndScan()
////            scan()
//        }

        _floatBtnLocal.setOnClickListener {
            Log.i(TAG, "_floatBtnLocal ")
            setPathAndScan()
//            getAllFilesInResources()
//            scan()
        }

        _swLoop.setOnCheckedChangeListener { _, isChecked ->
            isAutoPlay = isChecked
        }
        _autoTime.setSelection(0)
        _autoTime.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                Log.d(TAG, "_autoTime:${position}")
                DELAY_TIME = when (position) {
                    0 -> {
                        5 * 1000L
                    }
                    else -> {
                        position * 10 * 1000L
                    }
                }
                if (_player.isPlaying) {
                    _player.stop()
//                    _player.reset()
                    MainScope().launch {
                        delay(500)
                        playMedia()
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
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
            setDisplay(_playerView.holder)
        }
    }

    private fun prepareMediaPlayer(path: Uri, duration: Long) {
        // clear surface view
        _playerView.holder.setFormat(PixelFormat.TRANSPARENT)
        _playerView.holder.setFormat(PixelFormat.OPAQUE)
        var durationTime = duration

        try {
            _player.setDataSource(this, path)
//            _player.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT)
            _player.prepareAsync()

        } catch (e: IllegalStateException) {
            myOnError(1, 0)
        } catch (e: IOException) {
            myOnError(-1004, 0)
        } catch (e: RuntimeException) {
            myOnError(1, 0)
        }

        /** PLAYER STATUS **/
        _player.setOnPreparedListener {
            _player.start()

            mainViewModel.currentMediaInfo.isAbility = 3
            mediaListAdapter.notifyItemChanged(mainViewModel.currentPosition)

            Log.i(TAG, "!!!!!!!isPlaying:\n${it.metrics}")
            if (durationTime == -1L) {
                durationTime = _player.duration.toLong()
            }
            if (durationTime == 0L) {
                myOnError(-7788, 0)
            } else {
                _progress.max = durationTime.toInt()
                val delayTime =
                    if (durationTime < DELAY_TIME) durationTime else DELAY_TIME
                Log.i(TAG, "CountDownTimer: duration:$duration, delayTime:$delayTime")
                object : CountDownTimer(durationTime, 10) {
                    override fun onTick(millisUntilFinished: Long) {
                        if (mainViewModel.currentMediaInfo.isAbility == 2 ||
                            isFailed ||
                            !_player.isPlaying
                        ) {
                            this.cancel()
                        }
                        if (isAutoPlay && (millisUntilFinished <= (durationTime - delayTime))) {
                            this.cancel()
                            this.onFinish()
                            Log.i(TAG, "CountDownTimer millisUntilFinished: $millisUntilFinished")
                        }
                        _progress.progress = (durationTime - millisUntilFinished).toInt()
                    }

                    override fun onFinish() {
                        _progress.progress = 0
                        mainViewModel.currentMediaInfo.isAbility = 1
                        mediaListAdapter.notifyItemChanged(mainViewModel.currentPosition)
                        if (isAutoPlay) {
                            mainViewModel.saveLog("播放成功，自动切曲\n\n")
                            Log.i(TAG, "播放成功，自动切曲")
                        }
                        delayPlayNextMedia()
                    }
                }.start()
            }
        }

        _player.setOnVideoSizeChangedListener { player, width, height ->
            setSurfaceDimensions(player, width, height)
        }

        _player.setOnBufferingUpdateListener { _, _ ->
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

//        _player.setOnCompletionListener {
//            if (!isFailed) {
//                mainViewModel.currentMediaInfo.isAbility = 1
//                mediaListAdapter.notifyItemChanged(mainViewModel.currentPosition)
//                Log.i(TAG, "播放完成\n\n")
//                mainViewModel.saveLog("播放完成\n\n")
////                delayPlayNextMedia()
//            }
//        }
    }

    private fun myOnError(what: Int = 0, extra: Int = 0) {
        Log.wtf(
            TAG,
            "!!!!!!!ERROR: what:${mainViewModel.whatToString(true, what)} extra:$extra"
        )
        Toast.makeText(
            this@MainActivity,
            "Play ERROR: ${mainViewModel.whatToString(true, what)}",
            Toast.LENGTH_SHORT
        ).show()
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
        mainViewModel.currentMediaInfo.path?.let {
            mainViewModel.playErrorFileSet.add(it)
        }
        // 当出现IO错误（USB读取失败）时，就没必要再继续尝试播放了
        if (what == -1004 || what == 100) {
            isAutoPlay = false
            _swLoop.isChecked = false
            _player.reset()
        } else {
            delayPlayNextMedia()
        }

    }

    private fun initScan(isUsb: Boolean) {
        _editText.setText(mainViewModel.usbMessPath)
        _scanFile = ScanFileUtil(mainViewModel.usbMessPath)
//        }
//        else {
//            _editText.setText(mainViewModel.internalPath)
//            ScanFileUtil(mainViewModel.internalPath)
//        }
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
                Toast.makeText(
                    this@MainActivity,
                    "Scan Start",
                    Toast.LENGTH_SHORT
                ).show()
//                _player.clearMediaItems()
                _swLoop.isChecked = false
//                _floatBtn.isEnabled = false
                _floatBtnLocal.isEnabled = false
            }

            override fun scanComplete(timeConsuming: Long) {
//                Log.i(TAG, "Scan Done, files: ${mainViewModel.allMediaInfo.value}")
                if (timeConsuming > -1) {
                    Log.i(TAG, "Scan Complete")
                    Toast.makeText(
                        this@MainActivity,
                        "Scan Done, consumed: $timeConsuming",
                        Toast.LENGTH_SHORT
                    ).show()
                    MainScope().launch {
                        delay(timeConsuming * 3)
                        _floatBtnLocal.isEnabled = true
                        if (mainViewModel.allMediaInfo.value?.isNotEmpty() == true) {
                            _swLoop.isChecked = true
                            mainViewModel.currentPosition = 0
                            mainViewModel.saveLog(
                                "扫描结束，总计媒体文件数量：" + mainViewModel.allMediaInfo.value?.size
                            )
                            playMedia()
                        }
                    }
                }
            }

            override fun scanningCallBack(file: File) {
                Log.i(TAG, "Scan CallBack:  " + file.name)
                if (file.isFile && file.length() > 1024)
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

    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.i(TAG, "registerForActivityResult: $result")
            if (result.resultCode == Activity.RESULT_OK) {
                scan()
            } else {
                Toast.makeText(this@MainActivity, "NO Permission, NO Scan", Toast.LENGTH_SHORT)
                    .show()
            }
        }

    private fun scan() {
        Log.i(TAG, "scan()")
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) -> {
                if (mainViewModel.isExternalStorage) {
                    if (Environment.isExternalStorageManager()) {
                        Log.i(TAG, "isExternalStorageManager")
                        mainViewModel.initLogFile()
                        startScan()
                    } else {
                        Log.i(TAG, "NOT isExternalStorageManager")
                        try {
                            val intent = Intent()
                            intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
//                    intent.data = Uri.parse(packageName)
                            resultLauncher.launch(intent)
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
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
        Log.i(TAG, "Start Scan Path: ${mainViewModel.usbMessPath}")
//        if (!File(mainViewModel.usbMessPath).exists()) {
//            Log.e(TAG, "NO disk")
//            Toast.makeText(this@MainActivity, "NO USB disk", Toast.LENGTH_SHORT)
//                .show()
//            return
//        }
        _scanFile.stop()
        if (_player.isPlaying) {
            _player.stop()
//            _player.reset()
        }
        _scanFile.startAsyncScan()
    }

    /* 点击项目播放 */
    override fun onPlayListener(mediaInfo: MediaInfo, position: Int) {
        _swLoop.isChecked = false
        if (mainViewModel.currentPosition != -1 && mainViewModel.currentMediaInfo.isAbility == 3) {
            mainViewModel.currentMediaInfo.isAbility = 0
            mediaListAdapter.notifyItemChanged(mainViewModel.currentPosition)
        }
        mainViewModel.currentMediaInfo = mediaInfo
        mainViewModel.currentPosition = position

        if (_player.isPlaying)
            _player.stop()
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
//        _player.stop()
        _player.setDisplay(null)
        _player.reset()
        if ((mainViewModel.allMediaInfo.value?.size ?: -1) == -1) {
            _player.reset()
            return
        }
        if (mainViewModel.currentPosition >= (mainViewModel.allMediaInfo.value?.size ?: -1)) {
            if (isAutoPlay) {
                if (_infinityPlay.isChecked) {
                    mainViewModel.currentPosition = 0
                    playMedia()
                } else {
                    _player.stop()
                    _player.reset()
                    var errorFilePaths = "\n"
                    for (s: String in mainViewModel.playErrorFileSet) {
                        errorFilePaths += (s + "\n")
                    }
                    mainViewModel.saveLog(
                        "本次测试结束\n" +
                                "总计测试媒体数量：" + (mainViewModel.allMediaInfo.value?.size ?: 0) + "\n" +
                                "测试错误数量：" + mainViewModel.playErrorFileSet.size + "\n" +
                                "测试错误文件列表：" + errorFilePaths
                    )
                    mainViewModel.playErrorFileSet.clear()
                }
            }
        } else {
            // 重置失败状态
            isFailed = false
            mainViewModel.currentMediaInfo =
                mainViewModel.allMediaInfo.value!![mainViewModel.currentPosition]
            _recyclerView.smoothScrollToPosition(mainViewModel.currentPosition)

            if (!mainViewModel.currentMediaInfo.path.isNullOrEmpty()) {

                val path = mainViewModel.currentMediaInfo.path
                Log.i(
                    TAG,
                    "start_play: ${mainViewModel.currentPosition}, path: $path"
                )
                mainViewModel.saveLog("准备播放: $path")

//                if (mainViewModel.currentMediaInfo.path != null) {
                val mmr = MediaMetadataRetriever()
                var durationTime = 0L
                if (!path.isNullOrEmpty()) {
                    val inputStream = FileInputStream(File(path).absolutePath)
                    try {
                        mmr.setDataSource(inputStream.fd)
                        val id3Info = "File Path:" + path +
                                "\n\nTITLE:   " + (
                                mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                                    ?: "NO TITLE"
                                ).toUTF8String() +
                                "\nALBUM:   " + (
                                mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                                    ?: "NO ALBUM"
                                ).toUTF8String() +
                                "\nARTIST:   " + (
                                mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                                    ?: "NO ARTIST"
                                ).toUTF8String() +
                                "\nALBUM—ARTIST:   " + (
                                mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                                    ?: "NO ARTIST"
                                ).toUTF8String() +
                                "\nGENRE:   " + (
                                mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
                                    ?: "NO ARTIST"
                                ).toUTF8String() +
                                "\nAUTHOR:   " + (
                                mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR)
                                    ?: "NO ARTIST"
                                ).toUTF8String() +
                                "\nCOMPOSER:   " + (
                                mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER)
                                    ?: "NO ARTIST"
                                ).toUTF8String() + ("\n\n\n")
//                    mainViewModel.saveLog(id3Info)
                        _id3Info.text = id3Info
                        durationTime =
                            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                                ?.toLong()
                                ?: -1L

                        val cover = mainViewModel.getAlbumImage(mmr)
                        if (null != cover) {
                            Log.i(TAG, "get cover!")
                            _playerView.visibility = View.GONE
                            _cover.visibility = View.VISIBLE
                            _cover.setImageBitmap(cover)
                        } else {
                            _playerView.visibility = View.VISIBLE
                            _cover.visibility = View.GONE
                        }
                    } catch (e: FileNotFoundException) {
                        e.printStackTrace();
                    } catch (e: IOException) {
                        e.printStackTrace();
                    } catch (e: RuntimeException) {
                        e.printStackTrace();
                    } finally {
                        mmr.release()
                    }
                }

                prepareMediaPlayer(Uri.parse(path), durationTime)
            } else {
                Log.i(
                    TAG,
                    "play stop"
                )
                _player.stop()
                _player.reset()
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
        Log.i(TAG, "surfaceCreated")
        setupMediaPlayer(holder.surface)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
    }

    /*
    转换特殊编码String
     */
    private fun String.toUTF8String(): String {
        val ins = intern().byteInputStream()
        val head = ByteArray(3)
        ins.read(head)
//        Log.i(TAG, "code:${head[0]},${head[1]}")
        return when {
            //utf16
            head[0].toInt() == -28 && head[1].toInt() == -92 -> String(
                intern().toByteArray(Charsets.UTF_16),
                Charset.defaultCharset()
            ).substring(2)
            //utf32
            head[0].toInt() == -28 && head[1].toInt() == -124 -> String(
                intern().toByteArray(Charsets.UTF_32),
                Charset.defaultCharset()
            ).substring(2)
            // unicode
            head[0].toInt() == -2 && head[1].toInt() == -1 -> intern()
            // utf8
            head[0].toInt() == -17 && head[1].toInt() == -69 && head[2].toInt() == -65 -> intern()
            else -> intern()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _player.reset()
        _player.release()
    }
}