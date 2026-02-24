package com.example.imagegallery.adapter

import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.request.Disposable
import com.example.imagegallery.databinding.ItemImageFullBinding
import com.example.imagegallery.databinding.ItemVideoFullBinding
import com.example.imagegallery.model.ImageItem

/**
 * 全屏媒体滑动适配器，支持图片（PhotoView）和视频（VideoView）
 */
class ImagePagerAdapter(
    private val images: List<ImageItem>,
    private val onImageClick: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_IMAGE = 0
        private const val VIEW_TYPE_VIDEO = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (images[position].isVideo) VIEW_TYPE_VIDEO else VIEW_TYPE_IMAGE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_VIDEO) {
            val binding = ItemVideoFullBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            VideoViewHolder(binding)
        } else {
            val binding = ItemImageFullBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            ImageViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ImageViewHolder -> holder.bind(images[position])
            is VideoViewHolder -> holder.bind(images[position])
        }
    }

    override fun getItemCount(): Int = images.size

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is VideoViewHolder) {
            holder.release()
        }
    }

    // ---- 图片 ViewHolder ----
    inner class ImageViewHolder(
        private val binding: ItemImageFullBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var imageLoadDisposable: Disposable? = null

        init {
            binding.photoView.setOnClickListener {
                onImageClick()
            }
        }

        fun bind(item: ImageItem) {
            binding.progressBar.visibility = View.VISIBLE

            imageLoadDisposable?.dispose()
            imageLoadDisposable = binding.photoView.load(item.uri) {
                crossfade(true)
                listener(
                    onSuccess = { _, _ ->
                        binding.progressBar.visibility = View.GONE
                    },
                    onError = { _, _ ->
                        binding.progressBar.visibility = View.GONE
                    }
                )
            }
        }
    }

    // ---- 视频 ViewHolder ----
    inner class VideoViewHolder(
        private val binding: ItemVideoFullBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ImageItem) {
            binding.progressBar.visibility = View.VISIBLE

            val mediaController = MediaController(binding.root.context)
            mediaController.setAnchorView(binding.videoView)
            binding.videoView.setMediaController(mediaController)
            binding.videoView.setVideoURI(item.uri)

            binding.videoView.setOnPreparedListener { mp: MediaPlayer ->
                binding.progressBar.visibility = View.GONE
                // 自动播放
                mp.start()
            }

            binding.videoView.setOnErrorListener { _, _, _ ->
                binding.progressBar.visibility = View.GONE
                false
            }

            binding.videoView.requestFocus()
        }

        fun release() {
            binding.videoView.stopPlayback()
        }
    }
}
