package com.coolerfall.download

import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Environment
import android.os.Environment.DIRECTORY_DCIM
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.os.Environment.DIRECTORY_MOVIES
import android.os.Environment.DIRECTORY_MUSIC
import android.os.Environment.DIRECTORY_PICTURES
import java.io.OutputStream

/**
 * A [Pack] implementation which will save file to external storage public directory.
 * The type should be one of [DIRECTORY_MUSIC], [DIRECTORY_PICTURES], [DIRECTORY_MOVIES],
 * [DIRECTORY_DOWNLOADS] or [DIRECTORY_DCIM]
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
class ExtPublicPack(
	type: String = DIRECTORY_DOWNLOADS,
	subDir: String? = null,
	filename: String? = null
) : BreakpointPack(filename) {

	/* it's recommended to use media store api after Q */
	private val pack =
		if (VERSION.SDK_INT >= VERSION_CODES.Q) MediaPack(type, filename)
		else DirectFilePack(
			Helper.resolvePath(
				Environment.getExternalStoragePublicDirectory(type).absolutePath,
				subDir ?: ""
			),
			filename
		)

	override fun putFilenameIfAbsent(filename: () -> String) {
		pack.putFilenameIfAbsent(filename)
	}

	override fun pendingLength(): Long {
		return pack.pendingLength()
	}

	override fun open(): OutputStream {
		return pack.open()
	}

	override fun finish() {
		pack.finish()
	}
}