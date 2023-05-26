package com.coolerfall.download

import android.content.ContentResolver.QUERY_ARG_SQL_SELECTION
import android.content.ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS
import android.content.ContentValues
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.MediaStore.Audio
import android.provider.MediaStore.Downloads
import android.provider.MediaStore.Files.FileColumns
import android.provider.MediaStore.Images
import android.provider.MediaStore.Video
import android.webkit.MimeTypeMap
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.OutputStream

/**
 * A [Pack] implementation with [MediaStore] api. This pack is only available after Q.
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
@RequiresApi(VERSION_CODES.Q)
class MediaPack(
	private val type: String = Environment.DIRECTORY_DOWNLOADS,
	private val subDir: String? = null,
	filename: String? = null
) : BreakpointPack(filename) {

	private val contentUri = requireNotNull(URI_MAP[type]) { "Type [$type] not supported" }
	private val contentResolver = DownloadProvider.requireContext().contentResolver

	companion object {
		private val URI_MAP = mutableMapOf<String, Uri>(
			Pair(Environment.DIRECTORY_DOWNLOADS, Downloads.EXTERNAL_CONTENT_URI),
			Pair(Environment.DIRECTORY_PICTURES, Images.Media.EXTERNAL_CONTENT_URI),
			Pair(Environment.DIRECTORY_DCIM, Images.Media.EXTERNAL_CONTENT_URI),
			Pair(Environment.DIRECTORY_MOVIES, Video.Media.EXTERNAL_CONTENT_URI),
			Pair(Environment.DIRECTORY_MUSIC, Audio.Media.EXTERNAL_CONTENT_URI),
		)
	}

	override fun pendingLength(): Long {
		return queryPendingUri(uniqueFilename())?.second ?: 0
	}

	override fun open(): OutputStream {
		var uri = queryPendingUri(uniqueFilename())?.first
		/* no pending file found, create a new one */
		if (uri == null) {
			uri = createUri()
		}

		return contentResolver.openOutputStream(uri, "wa") ?: throw FileNotFoundException()
	}

	override fun finish() {
		val uri = queryPendingUri(uniqueFilename())?.first ?: return
		val contentValues = ContentValues()
		contentValues.put(FileColumns.DISPLAY_NAME, requireFilename())
		contentValues.put(FileColumns.IS_PENDING, false)
		contentResolver.update(uri, contentValues, null, null)
	}

	private fun queryPendingUri(filename: String): Pair<Uri, Long>? {
		val queryArgs = Bundle().apply {
			if (VERSION.SDK_INT >= VERSION_CODES.R) {
				putInt(MediaStore.QUERY_ARG_MATCH_PENDING, MediaStore.MATCH_ONLY)
			}
			putString(QUERY_ARG_SQL_SELECTION, FileColumns.DISPLAY_NAME + " = ?")
			putStringArray(QUERY_ARG_SQL_SELECTION_ARGS, arrayOf(filename))
		}
		@Suppress("DEPRECATION") val uri =
			if (VERSION.SDK_INT >= VERSION_CODES.R) contentUri
			else MediaStore.setIncludePending(contentUri)
		val cursor = contentResolver.query(
			uri,
			arrayOf(FileColumns._ID, FileColumns.DATA),
			queryArgs,
			null
		)

		if (cursor == null || cursor.count == 0 || !cursor.moveToFirst()) {
			return null
		}

		val id = cursor.getString(cursor.getColumnIndexOrThrow(FileColumns._ID))
		val path = cursor.getString(cursor.getColumnIndexOrThrow(FileColumns.DATA))
		cursor.close()

		return Pair(Uri.withAppendedPath(contentUri, id), File(path).length())
	}

	private fun createUri(): Uri {
		val filename = uniqueFilename()
		val contentValues = ContentValues()
		contentValues.put(FileColumns.RELATIVE_PATH, Helper.resolvePath(type, subDir ?: ""))
		contentValues.put(FileColumns.DISPLAY_NAME, filename)
		contentValues.put(FileColumns.IS_PENDING, true)
		val index = filename.lastIndexOf(".")
		if (index > 0) {
			val mimeType = MimeTypeMap.getSingleton()
				.getMimeTypeFromExtension(filename.substring(index + 1))
			if (mimeType != null) {
				contentValues.put(FileColumns.MIME_TYPE, mimeType)
			}
		}
		return contentResolver.insert(contentUri, contentValues)
			?: throw IOException("Cannot open output stream for file: $filename")
	}
}