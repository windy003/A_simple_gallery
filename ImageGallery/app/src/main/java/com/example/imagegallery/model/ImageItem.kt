package com.example.imagegallery.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 图片数据模型
 */
@Parcelize
data class ImageItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val path: String,
    val dateAdded: Long,
    val dateModified: Long,
    val size: Long,
    val isVideo: Boolean = false,
    val duration: Long = 0L
) : Parcelable
