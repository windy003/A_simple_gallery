package com.example.imagegallery.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.request.Disposable
import com.example.imagegallery.databinding.ItemImageFullBinding
import com.example.imagegallery.model.ImageItem

/**
 * 全屏图片滑动适配器
 */
class ImagePagerAdapter(
    private val images: List<ImageItem>,
    private val onImageClick: () -> Unit
) : RecyclerView.Adapter<ImagePagerAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemImageFullBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(images[position])
    }

    override fun getItemCount(): Int = images.size

    inner class ViewHolder(
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
}
