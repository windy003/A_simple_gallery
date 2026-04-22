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

class GalleryAdapter(
    private val onItemClick: (position: Int) -> Unit,
    private val onItemLongClick: (position: Int) -> Unit = {}
) : ListAdapter<ImageItem, GalleryAdapter.ViewHolder>(DiffCallback()) {

    var isSelectionMode = false
        private set
    private val selectedPositions = mutableSetOf<Int>()

    fun enterSelectionMode(position: Int) {
        isSelectionMode = true
        selectedPositions.clear()
        selectedPositions.add(position)
        notifyDataSetChanged()
    }

    fun exitSelectionMode() {
        isSelectionMode = false
        selectedPositions.clear()
        notifyDataSetChanged()
    }

    fun toggleSelection(position: Int) {
        if (selectedPositions.contains(position)) {
            selectedPositions.remove(position)
        } else {
            selectedPositions.add(position)
        }
        notifyItemChanged(position)
    }

    fun selectAll() {
        if (selectedPositions.size == itemCount) {
            selectedPositions.clear()
        } else {
            selectedPositions.clear()
            for (i in 0 until itemCount) {
                selectedPositions.add(i)
            }
        }
        notifyDataSetChanged()
    }

    fun getSelectedItems(): List<ImageItem> {
        return selectedPositions.mapNotNull { pos ->
            if (pos < itemCount) getItem(pos) else null
        }
    }

    fun getSelectedCount(): Int = selectedPositions.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGalleryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class ViewHolder(
        private val binding: ItemGalleryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    if (isSelectionMode) {
                        toggleSelection(position)
                        onItemLongClick(position)
                    } else {
                        onItemClick(position)
                    }
                }
            }

            binding.root.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    if (!isSelectionMode) {
                        enterSelectionMode(position)
                        onItemLongClick(position)
                    }
                }
                true
            }
        }

        fun bind(item: ImageItem, position: Int) {
            if (item.isVideo) {
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

            if (isSelectionMode) {
                binding.checkIcon.visibility = View.VISIBLE
                val isSelected = selectedPositions.contains(position)
                if (isSelected) {
                    binding.selectionOverlay.visibility = View.VISIBLE
                    binding.checkIcon.setImageResource(R.drawable.ic_check_circle)
                } else {
                    binding.selectionOverlay.visibility = View.GONE
                    binding.checkIcon.setImageResource(R.drawable.ic_uncheck_circle)
                }
            } else {
                binding.checkIcon.visibility = View.GONE
                binding.selectionOverlay.visibility = View.GONE
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
