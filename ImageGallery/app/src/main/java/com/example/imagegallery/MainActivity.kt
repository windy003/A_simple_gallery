package com.example.imagegallery

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.imagegallery.adapter.GalleryAdapter
import com.example.imagegallery.databinding.ActivityMainBinding
import com.example.imagegallery.model.ImageItem
import com.example.imagegallery.util.ImageLoader
import com.google.android.material.snackbar.Snackbar
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: GalleryAdapter
    private var mediaItems: List<ImageItem> = emptyList()

    private val executor = Executors.newSingleThreadExecutor()

    // 单权限请求（Android 12 及以下）
    private val requestSinglePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            loadMedia()
        } else {
            showPermissionDeniedMessage()
        }
    }

    // 多权限请求（Android 13+，同时申请图片和视频权限）
    private val requestMultiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val anyGranted = permissions.values.any { it }
        if (anyGranted) {
            loadMedia()
        } else {
            showPermissionDeniedMessage()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        checkPermissionAndLoad()
    }

    private fun setupRecyclerView() {
        adapter = GalleryAdapter { position ->
            openMediaViewer(position)
        }

        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 3)
            adapter = this@MainActivity.adapter
            setHasFixedSize(true)
        }
    }

    private fun checkPermissionAndLoad() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 分别检查图片和视频权限
            val imagePermission = Manifest.permission.READ_MEDIA_IMAGES
            val videoPermission = Manifest.permission.READ_MEDIA_VIDEO

            val imageGranted = ContextCompat.checkSelfPermission(this, imagePermission) == PackageManager.PERMISSION_GRANTED
            val videoGranted = ContextCompat.checkSelfPermission(this, videoPermission) == PackageManager.PERMISSION_GRANTED

            when {
                imageGranted && videoGranted -> loadMedia()
                shouldShowRequestPermissionRationale(imagePermission) ||
                shouldShowRequestPermissionRationale(videoPermission) -> {
                    showPermissionRationale {
                        requestMultiplePermissionsLauncher.launch(
                            arrayOf(imagePermission, videoPermission)
                        )
                    }
                }
                else -> {
                    requestMultiplePermissionsLauncher.launch(
                        arrayOf(imagePermission, videoPermission)
                    )
                }
            }
        } else {
            val permission = Manifest.permission.READ_EXTERNAL_STORAGE
            when {
                ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                    loadMedia()
                }
                shouldShowRequestPermissionRationale(permission) -> {
                    showPermissionRationale {
                        requestSinglePermissionLauncher.launch(permission)
                    }
                }
                else -> {
                    requestSinglePermissionLauncher.launch(permission)
                }
            }
        }
    }

    private fun loadMedia() {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyView.visibility = View.GONE

        executor.execute {
            val loaded = ImageLoader.loadMedia(this)

            runOnUiThread {
                binding.progressBar.visibility = View.GONE
                mediaItems = loaded

                if (mediaItems.isEmpty()) {
                    binding.emptyView.visibility = View.VISIBLE
                } else {
                    adapter.submitList(mediaItems)
                }
            }
        }
    }

    private fun openMediaViewer(position: Int) {
        val intent = Intent(this, ImageViewerActivity::class.java).apply {
            putParcelableArrayListExtra(ImageViewerActivity.EXTRA_IMAGES, ArrayList(mediaItems))
            putExtra(ImageViewerActivity.EXTRA_POSITION, position)
        }
        startActivity(intent)
    }

    private fun showPermissionRationale(onGrant: () -> Unit) {
        Snackbar.make(
            binding.root,
            R.string.permission_required,
            Snackbar.LENGTH_INDEFINITE
        ).setAction(R.string.grant_permission) {
            onGrant()
        }.show()
    }

    private fun showPermissionDeniedMessage() {
        Snackbar.make(
            binding.root,
            R.string.permission_denied,
            Snackbar.LENGTH_INDEFINITE
        ).setAction(R.string.open_settings) {
            openAppSettings()
        }.show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}
