package io.github.runnlin.exoplayerdemo

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

        fun bind(text: String?, type: String?) {
            mediaInfoName.text = "$text"
            if (isVideo(type)) {
                mediaInfoIcon.setImageResource(R.drawable.ic_video_library)
            } else {
                mediaInfoIcon.setImageResource(R.drawable.ic_audiotrack)
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
        fun onPlayListener(mediaInfo: MediaInfo)
        fun onInfoListener(infoString: String)
    }

    fun addItemClickListener(listener: onItemClickListener) {
        this.listeners.add(listener)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        return MediaViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current.title, current.type)
        holder.itemView.setOnClickListener {
            Log.d("RECYCLER", "click: $position, ${current.title}")
            if (listeners.size > 0) {
                for (listener in listeners) {
                    listener.onPlayListener(current)
//                    listener.onInfoListener(current.path)
                }
            }
        }
    }
}