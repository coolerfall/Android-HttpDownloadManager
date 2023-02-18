package com.coolerfall.download

import android.annotation.SuppressLint
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY

/**
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
@RestrictTo(LIBRARY)
class DownloadProvider : ContentProvider() {

	companion object {
		@SuppressLint("StaticFieldLeak")
		internal var appContext: Context? = null
	}

	override fun onCreate(): Boolean {
		appContext = context?.applicationContext
		return true
	}

	override fun query(
		uri: Uri,
		projection: Array<out String>?,
		selection: String?,
		selectionArgs: Array<out String>?,
		sortOrder: String?
	): Cursor? {
		return null
	}

	override fun getType(uri: Uri): String? {
		return null
	}

	override fun insert(uri: Uri, values: ContentValues?): Uri? {
		return null
	}

	override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
		return 0
	}

	override fun update(
		uri: Uri,
		values: ContentValues?,
		selection: String?,
		selectionArgs: Array<out String>?
	): Int {
		return 0
	}
}