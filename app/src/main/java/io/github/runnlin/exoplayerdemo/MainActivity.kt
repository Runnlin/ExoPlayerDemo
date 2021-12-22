package io.github.runnlin.exoplayerdemo

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.Toast
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
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.trackselection.TrackSelectionOverrides
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.common.collect.ImmutableSet
import io.github.runnlin.exoplayerdemo.data.MediaInfo
import io.github.runnlin.exoplayerdemo.databinding.ActivityMainBinding
import java.io.File


class MainActivity : AppCompatActivity(), MediaListAdapter.onItemClickListener, Player.Listener {

    companion object {
        private const val rootPath = "/storage/usb0/"

        //private const val rootPath = "/data/"
        private const val disableAudio = true
    }

    private lateinit var _recyclerView: RecyclerView
    private lateinit var _floatBtn: FloatingActionButton
    private lateinit var _playerView: PlayerView
    private lateinit var _player: ExoPlayer
    private lateinit var _binding: ActivityMainBinding
    private lateinit var _usbReceiver: BroadcastReceiver
    private var builderForInfoDialog: CustomDialog.Builder? = null

    //    private var _infoDialog: CustomDialog? = null
    private val _scanFile = ScanFileUtil(rootPath)

    private val mainViewModel: MainViewModel by viewModels {
        MediaViewModelFactory((application as ExpPlayerDemoApplication).repository)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(_binding.root)

        _usbReceiver = USBReceiver { usbDiskMountState ->
            when (usbDiskMountState) {
                USBReceiver.USB_DISK_MOUNTED -> {
                    scan()
                }
                USBReceiver.USB_DISK_UNMOUNTED -> {
                    stopScan()
                }
            }
        }
        initReceiver()
        initView()
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
        val usbDeviceStateFilter = IntentFilter()
        usbDeviceStateFilter.addAction(Intent.ACTION_MEDIA_MOUNTED)
        usbDeviceStateFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED)
        usbDeviceStateFilter.addDataScheme("file")
        registerReceiver(_usbReceiver, usbDeviceStateFilter)
    }

    private fun initView() {
        _recyclerView = _binding.rvPlaylist
        _playerView = _binding.videoView
        _floatBtn = _binding.floatBtn
        builderForInfoDialog = CustomDialog.Builder(this)
        val mediaListAdapter = MediaListAdapter()
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
            Log.i("MainActivity: ", "files: $medias")

            medias.let {
                mediaListAdapter.submitList(
                    listOf(
                        MediaInfo(
                            uuid = "netTest001",
                            type = "mp4",
                            title = "Network Media Test",
                            path = "https://v-cdn.zjol.com.cn/276982.mp4"
                        )
                    ) + medias
                )
            }
        })

        _player = ExoPlayer.Builder(this).build().apply {
            playWhenReady = true
            _playerView.player = this
//            repeatMode = Player.REPEAT_MODE_ALL
            addListener(this@MainActivity)
//            setAudioAttributes(AudioAttributes.DEFAULT, false)
        }

        _floatBtn.setOnClickListener {
            mainViewModel.deleteAll()
            scan()
        }
    }

    // Player Listener
    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        Toast.makeText(this, "Player ERROR: ${error.errorCodeName}", Toast.LENGTH_LONG).show()
        mainViewModel.currentMediaInfo.isAbility = 2
        mainViewModel.update(mainViewModel.currentMediaInfo)
        _player.seekToNextMediaItem()
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        super.onPlaybackStateChanged(playbackState)
        when (playbackState) {
            Player.STATE_ENDED -> Log.i("Main", "onPlaybackStateChanged: STATE_ENDED")
            Player.STATE_IDLE -> Log.i("Main", "onPlaybackStateChanged: STATE_IDLE")
            Player.STATE_BUFFERING -> Log.i("Main", "onPlaybackStateChanged: STATE_BUFFERING")
            Player.STATE_READY -> Log.i("Main", "onPlaybackStateChanged: STATE_READY")
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)
        Log.i("Main", "onIsPlayingChanged: ${if (isPlaying) "YES" else "NO"}")
    }

    private fun stopScan() {
        _scanFile.stop()
    }

    private fun scan() {

        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) -> {
                startScan()
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
        _scanFile.setCallBackFilter(
            ScanFileUtil.FileFilterBuilder().apply {
                scanVideoFiles()
                scanMusicFiles()
            }.build()
        )
        _scanFile.setScanFileListener(object : ScanFileUtil.ScanFileListener {
            override fun scanBegin() {
                Toast.makeText(this@MainActivity, "Scan Started!", Toast.LENGTH_SHORT).show()
                Log.i("Main", "Scan Start")
            }

            override fun scanComplete(timeConsuming: Long) {
                Log.i("Main", "Scan Done, consumed: $timeConsuming")
                Toast.makeText(
                    this@MainActivity,
                    "Scan Done, consumed: $timeConsuming",
                    Toast.LENGTH_SHORT
                ).show()
                mainViewModel.allMediaInfo.value.let {
                    if (it != null) {
                        for (mediaInfo in it) {
                            val playItem = mediaInfo.path?.let { it1 -> MediaItem.fromUri(it1) }
                            if (playItem != null) {
                                _player.addMediaItem(playItem)
                            }
                        }
                    }
                }
//                _player.prepare()
//                _recyclerView.isClickable = false
            }

            override fun scanningCallBack(file: File) {
                mainViewModel.insert(packageMediaFile(file))
            }
        })
        _scanFile.startAsyncScan()
    }

    fun packageMediaFile(file: File): MediaInfo {
        return MediaInfo(
            file.name,
            file.extension,
            file.length().toInt(),
            file.name,
            file.length().toInt(),
            file.path
//            if (file.path.startsWith("/mnt/media_rw/usb0/")) rootPath + "video/mp4/" + file.name else file.path
        )
    }

    override fun onPlayListener(mediaInfo: MediaInfo) {
        mainViewModel.currentMediaInfo = mediaInfo
        playMedia()
    }

    private fun playMedia() {
        Log.i("MainActivity: ", "start play: ${mainViewModel.currentMediaInfo.path}")
        if (mainViewModel.currentMediaInfo.path?.isNotEmpty() == true) {

            val mediaItem = MediaItem.fromUri(Uri.parse(mainViewModel.currentMediaInfo.path))
//        val mediaItem = MediaItem.fromUri(Uri.parse("/data/1mute.h264.mp4"))
            _player.setMediaItem(mediaItem)

            if (disableAudio)
                _player.trackSelectionParameters.buildUpon()
                    .setDisabledTrackTypes(ImmutableSet.of(C.TRACK_TYPE_AUDIO))
                    .build()
            _player.prepare()
//            val mmr = MediaMetadataRetriever()
//            mmr.setDataSource(mainViewModel.currentMediaInfo.path)
//            Log.i("mmr:",mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: "NO TITLE")
//            Log.i("mmr:",mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "NO ALBUM")
//            showInfoDialog(
//                mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: "NO TITLE",
//                mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "NO ALBUM"
//            )

        }
    }

//    private fun showInfoDialog(title: String, message: String) {
//        _infoDialog =
//            builderForInfoDialog!!.setTitle(title).setMessage(message).createSingleButtonDialog()
//        _infoDialog!!.show()
//    }

}