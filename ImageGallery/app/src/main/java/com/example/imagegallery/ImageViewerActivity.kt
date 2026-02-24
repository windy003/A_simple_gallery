package com.example.imagegallery

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.viewpager2.widget.ViewPager2
import com.example.imagegallery.adapter.ImagePagerAdapter
import com.example.imagegallery.databinding.ActivityImageViewerBinding
import com.example.imagegallery.model.ImageItem
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ImageViewerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_IMAGES = "extra_images"
        const val EXTRA_POSITION = "extra_position"
    }

    private lateinit var binding: ActivityImageViewerBinding
    private var images: List<ImageItem> = emptyList()
    private var isUiVisible = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityImageViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSystemUi()
        loadData()
        setupViewPager()
        setupClickListeners()
    }

    private fun setupSystemUi() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
            val defaultMargin = (8 * resources.displayMetrics.density).toInt()

            binding.btnBack.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = insets.top + defaultMargin
            }
            binding.btnInfo.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = insets.top + defaultMargin
            }
            binding.imageCounter.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = insets.top + defaultMargin
            }

            windowInsets
        }

        hideSystemUi()
    }

    private fun hideSystemUi() {
        val windowInsetsController = WindowCompat.getInsetsController(window, binding.root)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsController.systemBarsBehavior =
            androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        isUiVisible = false
    }

    private fun showSystemUi() {
        val windowInsetsController = WindowCompat.getInsetsController(window, binding.root)
        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        isUiVisible = true
    }

    private fun toggleUi() {
        if (isUiVisible) {
            hideSystemUi()
            binding.imageCounter.visibility = View.GONE
            binding.btnBack.visibility = View.GONE
            binding.btnInfo.visibility = View.GONE
        } else {
            showSystemUi()
            if (images.size > 1) {
                binding.imageCounter.visibility = View.VISIBLE
            }
            binding.btnBack.visibility = View.VISIBLE
            binding.btnInfo.visibility = View.VISIBLE
        }
    }

    private fun loadData() {
        if (intent.action == android.content.Intent.ACTION_VIEW && intent.data != null) {
            val uri = intent.data!!
            val mimeType = intent.type ?: contentResolver.getType(uri) ?: ""
            val isVideo = mimeType.startsWith("video/")

            val externalItem = ImageItem(
                id = 0,
                uri = uri,
                name = getFileNameFromUri(uri),
                path = uri.path ?: "",
                dateAdded = 0,
                dateModified = 0,
                size = 0,
                isVideo = isVideo
            )
            images = listOf(externalItem)
        } else {
            images = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableArrayListExtra(EXTRA_IMAGES, ImageItem::class.java) ?: emptyList()
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra(EXTRA_IMAGES) ?: emptyList()
            }
        }
    }

    private fun getFileNameFromUri(uri: android.net.Uri): String {
        var name = ""
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        name = cursor.getString(nameIndex) ?: ""
                    }
                }
            }
        }
        if (name.isEmpty()) {
            name = uri.lastPathSegment ?: "Unknown"
        }
        return name
    }

    private fun setupViewPager() {
        val startPosition = intent.getIntExtra(EXTRA_POSITION, 0)

        val adapter = ImagePagerAdapter(images) {
            toggleUi()
        }

        binding.viewPager.apply {
            this.adapter = adapter
            setCurrentItem(startPosition, false)
            offscreenPageLimit = 1

            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    updateCounter(position)
                }
            })
        }

        updateCounter(startPosition)
    }

    private fun updateCounter(position: Int) {
        if (images.size <= 1) {
            binding.imageCounter.visibility = View.GONE
        } else {
            binding.imageCounter.text = "${position + 1} / ${images.size}"
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnInfo.setOnClickListener {
            showMediaInfo()
        }
    }

    private fun showMediaInfo() {
        val currentPosition = binding.viewPager.currentItem
        if (currentPosition < 0 || currentPosition >= images.size) return

        val item = images[currentPosition]
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val unknown = getString(R.string.info_unknown)

        val dateAdded = if (item.dateAdded > 0) dateFormat.format(Date(item.dateAdded * 1000)) else unknown
        val dateModified = if (item.dateModified > 0) dateFormat.format(Date(item.dateModified * 1000)) else unknown
        val fileSize = if (item.size > 0) formatFileSize(item.size) else unknown
        val path = item.path.ifEmpty { item.uri.toString() }

        val durationLine = if (item.isVideo && item.duration > 0) {
            "\n\n${getString(R.string.video_duration)}:\n${formatDuration(item.duration)}"
        } else ""

        val message = """
            |${getString(R.string.info_name)}:
            |${item.name}
            |
            |${getString(R.string.info_path)}:
            |$path
            |
            |${getString(R.string.info_size)}:
            |$fileSize
            |
            |${getString(R.string.info_date_added)}:
            |$dateAdded
            |
            |${getString(R.string.info_date_modified)}:
            |$dateModified$durationLine
        """.trimMargin()

        val title = if (item.isVideo) R.string.media_info else R.string.image_info

        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.close, null)
            .show()
    }

    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }

    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> String.format(Locale.getDefault(), "%.2f KB", size / 1024.0)
            size < 1024 * 1024 * 1024 -> String.format(Locale.getDefault(), "%.2f MB", size / (1024.0 * 1024))
            else -> String.format(Locale.getDefault(), "%.2f GB", size / (1024.0 * 1024 * 1024))
        }
    }
}
