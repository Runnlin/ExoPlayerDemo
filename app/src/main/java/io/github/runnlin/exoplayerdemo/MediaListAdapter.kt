package io.github.runnlin.exoplayerdemo

import android.annotation.SuppressLint
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.runnlin.exoplayerdemo.data.MediaInfo
import java.util.*
import kotlin.collections.ArrayList


class MediaListAdapter :
    ListAdapter<MediaInfo, MediaListAdapter.MediaViewHolder>(MediaComparator()) {

    private var listeners = ArrayList<onItemClickListener>()

    class MediaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val mediaInfoName: TextView = itemView.findViewById(R.id.tv_item_name)
        private val mediaInfoIcon: ImageView = itemView.findViewById(R.id.iv_icon)
        private val mediaInfo: LinearLayout = itemView.findViewById(R.id.ll_info)
        private val mediaCheckIcon: ImageView = itemView.findViewById(R.id.iv_info)

        @SuppressLint("ResourceAsColor")
        fun bind(current: MediaInfo) {
            mediaInfoName.text = current.title
            if (isVideo(current.type)) {
                mediaInfoIcon.setImageResource(R.drawable.ic_video_library)
            } else {
                mediaInfoIcon.setImageResource(R.drawable.ic_audiotrack)
            }
            when (current.isAbility) {
                0 -> {
                    mediaInfo.setBackgroundColor(Color.GRAY)
                    mediaCheckIcon.setImageResource(R.drawable.ic_check_ready)
                }
                1 -> {
                    mediaInfo.setBackgroundColor(Color.GREEN)
                    mediaCheckIcon.setImageResource(R.drawable.ic_check_ok)
                }
                2 -> {
                    mediaInfo.setBackgroundColor(Color.RED)
                    mediaCheckIcon.setImageResource(R.drawable.ic_check_no)
                }
            }
        }

        fun isVideo(type: String?): Boolean {
            when (type?.lowercase(Locale.getDefault())) {
                "mp4", "avi", "flv", "3gp", "mkv", "wmv" -> return true
            }
            return false
        }

        companion object {
            fun create(parent: ViewGroup): MediaViewHolder {
                val view: View = LayoutInflater.from(parent.context)
                    .inflate(R.layout.playlist_item, parent, false)
                return MediaViewHolder(view)
            }
        }
    }

    class MediaComparator : DiffUtil.ItemCallback<MediaInfo>() {
        override fun areItemsTheSame(oldItem: MediaInfo, newItem: MediaInfo): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: MediaInfo, newItem: MediaInfo): Boolean {
            return oldItem.uuid == newItem.uuid
        }
    }


    interface onItemClickListener {
        fun onPlayListener(mediaInfo: MediaInfo, position: Int)
    }

    fun addItemClickListener(listener: onItemClickListener) {
        this.listeners.add(listener)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        return MediaViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current)
        holder.itemView.setOnClickListener {
            Log.d("RECYCLER", "click: $position, ${current.title}")
            if (listeners.size > 0) {
                for (listener in listeners) {
                    listener.onPlayListener(current, position)
                }
            }
        }
    }
}