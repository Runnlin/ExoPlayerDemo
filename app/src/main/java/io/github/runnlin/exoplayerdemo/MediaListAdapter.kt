package io.github.runnlin.exoplayerdemo

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.runnlin.exoplayerdemo.data.MediaInfo


class MediaListAdapter : ListAdapter<MediaInfo, MediaListAdapter.MediaViewHolder>(MediaComparator()) {

    var listeners = ArrayList<onItemClickListener>()

    class MediaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val mediaInfoName: TextView = itemView.findViewById(R.id.tv_item_name)
//        private val mediaInfoIcon: ImageView = itemView.findViewById(R.id.iv_icon)

        fun bind(text: String?, type: String?) {
            mediaInfoName.text = "$text"
//            when (type) {
//                "mp4","avi","flv" -> {
//                    mediaInfoIcon.setColorFilter(Color.RED)
//                }
//                else -> {
//                    mediaInfoIcon.setColorFilter(Color.BLUE)
//                }
//            }
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
        fun onItemClickListener(position: Int, mediaInfo: MediaInfo)
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
                for (listener in listeners)
                    listener.onItemClickListener(position, current)
            }
        }
    }
}