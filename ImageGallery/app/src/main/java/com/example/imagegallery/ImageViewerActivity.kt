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

        // 启用边到边显示
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityImageViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSystemUi()
        loadData()
        setupViewPager()
        setupClickListeners()
    }

    private fun setupSystemUi() {
        // 处理状态栏边距
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
            // 只有多张图片时显示计数器
            if (images.size > 1) {
                binding.imageCounter.visibility = View.VISIBLE
            }
            binding.btnBack.visibility = View.VISIBLE
            binding.btnInfo.visibility = View.VISIBLE
        }
    }

    private fun loadData() {
        // 检查是否从外部应用打开（文件浏览器等）
        if (intent.action == android.content.Intent.ACTION_VIEW && intent.data != null) {
            val uri = intent.data!!
            val externalImage = ImageItem(
                id = 0,
                uri = uri,
                name = getFileNameFromUri(uri),
                path = uri.path ?: "",
                dateAdded = 0,
                dateModified = 0,
                size = 0
            )
            images = listOf(externalImage)

            // 尝试获取更多文件信息
            tryLoadExternalImageInfo(uri)
        } else {
            // 从应用内部打开
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

    private fun tryLoadExternalImageInfo(uri: android.net.Uri) {
        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    val size = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L

                    val dateModifiedIndex = cursor.getColumnIndex("date_modified")
                    val dateModified = if (dateModifiedIndex >= 0) cursor.getLong(dateModifiedIndex) else 0L

                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    val name = if (nameIndex >= 0) cursor.getString(nameIndex) ?: "" else ""

                    if (images.isNotEmpty()) {
                        val current = images[0]
                        images = listOf(current.copy(
                            name = name.ifEmpty { current.name },
                            size = size,
                            dateModified = dateModified
                        ))
                    }
                }
            }
        } catch (_: Exception) {
            // 忽略错误，使用默认值
        }
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
            showImageInfo()
        }
    }

    private fun showImageInfo() {
        val currentPosition = binding.viewPager.currentItem
        if (currentPosition < 0 || currentPosition >= images.size) return

        val image = images[currentPosition]
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val unknown = getString(R.string.info_unknown)

        val dateAdded = if (image.dateAdded > 0) dateFormat.format(Date(image.dateAdded * 1000)) else unknown
        val dateModified = if (image.dateModified > 0) dateFormat.format(Date(image.dateModified * 1000)) else unknown
        val fileSize = if (image.size > 0) formatFileSize(image.size) else unknown
        val path = image.path.ifEmpty { image.uri.toString() }

        val message = """
            |${getString(R.string.info_name)}:
            |${image.name}
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
            |$dateModified
        """.trimMargin()

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.image_info)
            .setMessage(message)
            .setPositiveButton(R.string.close, null)
            .show()
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
