package com.example.imagegallery

import android.Manifest
import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.imagegallery.adapter.GalleryAdapter
import com.example.imagegallery.databinding.ActivityMainBinding
import com.example.imagegallery.model.ImageItem
import com.example.imagegallery.util.ImageLoader
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: GalleryAdapter
    private var mediaItems: List<ImageItem> = emptyList()

    private val executor = Executors.newSingleThreadExecutor()

    // 批量删除相关
    private var pendingDeleteItems: List<ImageItem> = emptyList()
    private var pendingDeleteIndex = 0

    private val requestSinglePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            loadMedia()
        } else {
            showPermissionDeniedMessage()
        }
    }

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

    private val deleteRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            onBatchDeleteSuccess()
        } else {
            Toast.makeText(this, R.string.delete_failed, Toast.LENGTH_SHORT).show()
            exitSelectionMode()
        }
    }

    // 用于在 ImageViewerActivity 删除后刷新列表
    private val viewerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            loadMedia()
        }
    }

    private val backPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            exitSelectionMode()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(this, backPressedCallback)

        setupRecyclerView()
        setupSelectionBar()
        checkPermissionAndLoad()
    }

    private fun setupRecyclerView() {
        adapter = GalleryAdapter(
            onItemClick = { position ->
                openMediaViewer(position)
            },
            onItemLongClick = { _ ->
                updateSelectionUi()
            }
        )

        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 3)
            adapter = this@MainActivity.adapter
            setHasFixedSize(true)
        }
    }

    private fun setupSelectionBar() {
        binding.btnDeleteSelected.setOnClickListener {
            confirmBatchDelete()
        }

        binding.btnSelectAll.setOnClickListener {
            adapter.selectAll()
            updateSelectionUi()
        }
    }

    private fun updateSelectionUi() {
        if (adapter.isSelectionMode) {
            val count = adapter.getSelectedCount()
            binding.selectionBar.visibility = View.VISIBLE
            binding.selectionCount.text = getString(R.string.selected_count, count)
            backPressedCallback.isEnabled = true

            if (count == 0) {
                exitSelectionMode()
            }
        } else {
            binding.selectionBar.visibility = View.GONE
            backPressedCallback.isEnabled = false
        }
    }

    private fun exitSelectionMode() {
        adapter.exitSelectionMode()
        binding.selectionBar.visibility = View.GONE
        backPressedCallback.isEnabled = false
    }

    private fun confirmBatchDelete() {
        val selectedItems = adapter.getSelectedItems()
        if (selectedItems.isEmpty()) return

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_confirm_title)
            .setMessage(getString(R.string.delete_batch_confirm_message, selectedItems.size))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                deleteBatch(selectedItems)
            }
            .show()
    }

    private fun deleteBatch(items: List<ImageItem>) {
        pendingDeleteItems = items

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+: 使用系统批量删除请求
            val uris = items.map { it.uri }
            val deleteRequest = MediaStore.createDeleteRequest(contentResolver, uris)
            deleteRequestLauncher.launch(IntentSenderRequest.Builder(deleteRequest.intentSender).build())
        } else {
            // Android 10 及以下: 逐个删除
            pendingDeleteIndex = 0
            deleteNextItem()
        }
    }

    private fun deleteNextItem() {
        if (pendingDeleteIndex >= pendingDeleteItems.size) {
            onBatchDeleteSuccess()
            return
        }

        val item = pendingDeleteItems[pendingDeleteIndex]
        try {
            contentResolver.delete(item.uri, null, null)
            pendingDeleteIndex++
            deleteNextItem()
        } catch (e: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
                val intentSender = e.userAction.actionIntent.intentSender
                deleteRequestLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            } else {
                pendingDeleteIndex++
                deleteNextItem()
            }
        }
    }

    private fun onBatchDeleteSuccess() {
        Toast.makeText(this, R.string.deleted_successfully, Toast.LENGTH_SHORT).show()
        exitSelectionMode()
        loadMedia()
    }

    private fun checkPermissionAndLoad() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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
        viewerLauncher.launch(intent)
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
