package io.github.runnlin.exoplayerdemo

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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

private val rootPath = "/storage/usb0/"

class MainActivity : AppCompatActivity(), MediaListAdapter.onItemClickListener, Player.Listener {

    private lateinit var _recyclerView: RecyclerView
    private lateinit var _floatBtn: FloatingActionButton
    private lateinit var playerView: PlayerView
    private lateinit var player: ExoPlayer
    private lateinit var binding: ActivityMainBinding


    private val mainViewModel: MainViewModel by viewModels {
        MediaViewModelFactory((application as ExpPlayerDemoApplication).repository)
    }

    val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                playMedia()
            } else {
                Toast.makeText(this@MainActivity, "NOT Permission, NOT Play", Toast.LENGTH_SHORT)
                    .show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
        scan()
    }

    fun initView() {
        _recyclerView = binding.rvPlaylist
        playerView = binding.videoView
        _floatBtn = binding.btnFloatbtn
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
                            "NetMediaFileTest",
                            "extension000",
                            30,
                            "Network Media Test",
                            30,
                            "https://v-cdn.zjol.com.cn/276982.mp4"
                        )
                    ) + medias
                )
            }
        })

        player = ExoPlayer.Builder(this).build().apply {
            this.playWhenReady = true
            playerView.player = this
            addListener(this@MainActivity)
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
    }

    fun scan() {
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
                Toast.makeText(this@MainActivity, "Scan Done, consumed: $timeConsuming", Toast.LENGTH_SHORT).show()
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

    override fun onItemClickListener(position: Int, mediaInfo: MediaInfo) {
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

    private fun playMedia() {
        Log.i("MainActivity: ", "start play: ${mainViewModel.currentMediaInfo.path}")
        val mediaItem = MediaItem.fromUri(Uri.parse(mainViewModel.currentMediaInfo.path))
        player.setMediaItem(mediaItem)
        player.prepare()
    }

}