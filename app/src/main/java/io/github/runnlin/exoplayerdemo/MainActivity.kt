package io.github.runnlin.exoplayerdemo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.Button
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
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.github.runnlin.exoplayerdemo.data.MediaInfo
import io.github.runnlin.exoplayerdemo.databinding.ActivityMainBinding
import java.io.File

private const val rootPath = "/storage/usb0/"
//private const val rootPath = "/data/"

class MainActivity : AppCompatActivity(), MediaListAdapter.onItemClickListener {

    private lateinit var _recyclerView: RecyclerView
    private lateinit var _floatBtn: FloatingActionButton
    private lateinit var _surface: SurfaceView
    private lateinit var _mediaPlayer: MediaPlayer
//    private lateinit var _btnStart: Button
//    private lateinit var _btnStop: Button
//    private lateinit var _btnPause: Button
    private lateinit var _binding: ActivityMainBinding

    private val mainViewModel: MainViewModel by viewModels {
        MediaViewModelFactory((application as ExpPlayerDemoApplication).repository)
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                playMedia()
            } else {
                Toast.makeText(this@MainActivity, "NO Permission, NO Play", Toast.LENGTH_SHORT)
                    .show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(_binding.root)
        initView()
//        scan()
    }

    override fun onResume() {
        super.onResume()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, _binding.root).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun initView() {
        _recyclerView = _binding.rvPlaylist
//        _btnStart = _binding.btnStart
//        _btnPause = _binding.btnPause
//        _btnStop = _binding.btnStop

        _floatBtn = _binding.btnFloatbtn
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

        _surface = _binding.surfaceView.apply {
            this.holder.setKeepScreenOn(true)
        }
        _mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
//            setDataSource(applicationContext, Uri.parse("https://v-cdn.zjol.com.cn/276982.mp4"))
//            prepare()
//            start()
        }

        _surface.holder.setKeepScreenOn(true)


        _floatBtn.setOnClickListener {
            mainViewModel.deleteAll()
            scan()
        }
    }

    private fun scan() {
        val scanFile = ScanFileUtil(rootPath)
        scanFile.setCallBackFilter(
            ScanFileUtil.FileFilterBuilder().apply {
                scanVideoFiles()
                scanMusicFiles()
//                addCustomFilter(FilenameFilter { dir, name ->
//                    return dir.length() != 0L
//                })
            }.build()
        )
        scanFile.setScanFileListener(object : ScanFileUtil.ScanFileListener {
            override fun scanBegin() {
                Toast.makeText(this@MainActivity, "Scan Started!", Toast.LENGTH_SHORT).show()
            }

            override fun scanComplete(timeConsuming: Long) {
                Toast.makeText(
                    this@MainActivity,
                    "Scan Done, consumed: $timeConsuming",
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun scanningCallBack(file: File) {
                mainViewModel.insert(packageMediaFile(file))
            }
        })
        scanFile.startAsyncScan()
    }

    fun packageMediaFile(file: File): MediaInfo {
        return MediaInfo(
            file.name,
            file.extension,
            file.length().toInt(),
            file.name,
            file.length().toInt(),
            file.path
        )
    }

    override fun onPlayListener(mediaInfo: MediaInfo) {
        mainViewModel.currentMediaInfo = mediaInfo

        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) -> {
                playMedia()
            }
            else -> {
                requestPermissionLauncher.launch(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            }
        }
    }

    override fun onInfoListener(infoString: String) {
        val intent = Intent(this, PopUpWindow::class.java)
        intent.putExtra("info", infoString)
        startActivity(intent)
    }

    private fun playMedia() {
        Log.i("MainActivity: ", "start play: ${mainViewModel.currentMediaInfo.path}")
//        _mediaPlayer.setDataSource(this, Uri.parse(mainViewModel.currentMediaInfo.path))
        _mediaPlayer.setDataSource(this, Uri.parse("/data/1.mp4"))
        _mediaPlayer.prepare()
        _mediaPlayer.start()
    }

}