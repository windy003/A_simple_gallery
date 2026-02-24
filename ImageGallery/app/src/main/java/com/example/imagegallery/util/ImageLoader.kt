package com.example.imagegallery.util

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.example.imagegallery.model.ImageItem

/**
 * 媒体加载工具类
 * 使用 MediaStore API 查询设备中的图片和视频
 */
object ImageLoader {

    /**
     * 从设备加载所有图片和视频，按添加时间倒序排列
     */
    fun loadMedia(context: Context): List<ImageItem> {
        val mediaList = mutableListOf<ImageItem>()
        mediaList.addAll(loadImages(context))
        mediaList.addAll(loadVideos(context))
        mediaList.sortByDescending { it.dateAdded }
        return mediaList
    }

    private fun loadImages(context: Context): List<ImageItem> {
        val images = mutableListOf<ImageItem>()

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.SIZE
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn) ?: ""
                val path = cursor.getString(pathColumn) ?: ""
                val dateAdded = cursor.getLong(dateAddedColumn)
                val dateModified = cursor.getLong(dateModifiedColumn)
                val size = cursor.getLong(sizeColumn)

                val uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                images.add(ImageItem(id, uri, name, path, dateAdded, dateModified, size, isVideo = false))
            }
        }

        return images
    }

    private fun loadVideos(context: Context): List<ImageItem> {
        val videos = mutableListOf<ImageItem>()

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn) ?: ""
                val path = cursor.getString(pathColumn) ?: ""
                val dateAdded = cursor.getLong(dateAddedColumn)
                val dateModified = cursor.getLong(dateModifiedColumn)
                val size = cursor.getLong(sizeColumn)
                val duration = cursor.getLong(durationColumn)

                val uri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                videos.add(ImageItem(id, uri, name, path, dateAdded, dateModified, size, isVideo = true, duration = duration))
            }
        }

        return videos
    }
}
