package com.example.imagegallery.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.decode.VideoFrameDecoder
import com.example.imagegallery.R
import com.example.imagegallery.databinding.ItemGalleryBinding
import com.example.imagegallery.model.ImageItem

/**
 * 网格视图适配器，支持图片和视频
 */
class GalleryAdapter(
    private val onItemClick: (position: Int) -> Unit
) : ListAdapter<ImageItem, GalleryAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGalleryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemGalleryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(position)
                }
            }
        }

        fun bind(item: ImageItem) {
            if (item.isVideo) {
                // 用 coil-video 加载视频第一帧作为缩略图
                binding.imageView.load(item.uri) {
                    crossfade(true)
                    decoderFactory { result, options, _ ->
                        VideoFrameDecoder(result.source, options)
                    }
                    placeholder(R.drawable.placeholder)
                    error(R.drawable.placeholder)
                }
                binding.playIcon.visibility = View.VISIBLE
            } else {
                binding.imageView.load(item.uri) {
                    crossfade(true)
                    placeholder(R.drawable.placeholder)
                    error(R.drawable.placeholder)
                }
                binding.playIcon.visibility = View.GONE
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<ImageItem>() {
        override fun areItemsTheSame(oldItem: ImageItem, newItem: ImageItem): Boolean {
            return oldItem.id == newItem.id && oldItem.isVideo == newItem.isVideo
        }

        override fun areContentsTheSame(oldItem: ImageItem, newItem: ImageItem): Boolean {
            return oldItem == newItem
        }
    }
}
