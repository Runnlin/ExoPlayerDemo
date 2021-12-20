package io.github.runnlin.exoplayerdemo

import android.os.Bundle
import android.os.PersistableBundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class PopUpWindow: AppCompatActivity() {
    private lateinit var btnClose: Button
    private lateinit var tvInfo: TextView

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        overridePendingTransition(0,0)
        setContentView(R.layout.media_info)

        val bundle = intent.extras

        btnClose = this.findViewById(R.id.iv_close)
        tvInfo = this.findViewById(R.id.tv_mediainfo)

        btnClose.setOnClickListener {
            finish()
        }
        tvInfo.text = bundle?.getString("info", "none") ?: ""
    }
}