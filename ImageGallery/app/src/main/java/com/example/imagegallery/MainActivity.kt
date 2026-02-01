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
    private var images: List<ImageItem> = emptyList()

    private val executor = Executors.newSingleThreadExecutor()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            loadImages()
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
            openImageViewer(position)
        }

        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 3)
            adapter = this@MainActivity.adapter
            setHasFixedSize(true)
        }
    }

    private fun checkPermissionAndLoad() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                loadImages()
            }
            shouldShowRequestPermissionRationale(permission) -> {
                showPermissionRationale(permission)
            }
            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    private fun loadImages() {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyView.visibility = View.GONE

        executor.execute {
            val loadedImages = ImageLoader.loadImages(this)

            runOnUiThread {
                binding.progressBar.visibility = View.GONE
                images = loadedImages

                if (images.isEmpty()) {
                    binding.emptyView.visibility = View.VISIBLE
                } else {
                    adapter.submitList(images)
                }
            }
        }
    }

    private fun openImageViewer(position: Int) {
        val intent = Intent(this, ImageViewerActivity::class.java).apply {
            putParcelableArrayListExtra(ImageViewerActivity.EXTRA_IMAGES, ArrayList(images))
            putExtra(ImageViewerActivity.EXTRA_POSITION, position)
        }
        startActivity(intent)
    }

    private fun showPermissionRationale(permission: String) {
        Snackbar.make(
            binding.root,
            R.string.permission_required,
            Snackbar.LENGTH_INDEFINITE
        ).setAction(R.string.grant_permission) {
            requestPermissionLauncher.launch(permission)
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
