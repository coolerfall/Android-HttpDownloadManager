package com.coolerfall.download

import android.os.Environment

/**
 * A [Pack] implementation which will save file to external files directory.
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
class ExtFilePack(
	type: String = Environment.DIRECTORY_DOWNLOADS,
	subDir: String? = null,
	filename: String? = null
) : DirectFilePack(
	Helper.resolvePath(
		requireNotNull(DownloadProvider.requireContext().getExternalFilesDir(type)) {
			"external storage is not currently available"
		}.absolutePath, subDir ?: ""
	), filename
)