package com.coolerfall.download

import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * A [Pack] with direct [File] api operation.
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
open class DirectFilePack(
	private val dir: String, filename: String? = null
) : BreakpointPack(filename) {

	init {
		val directory = File(dir)
		if (!directory.exists()) {
			directory.mkdirs()
		}
	}

	override fun pendingLength(): Long {
		return File(pendingPath()).length()
	}

	override fun open(): OutputStream {
		return FileOutputStream(pendingPath(), true)
	}

	override fun finish() {
		val filepath = Helper.resolvePath(dir, requireFilename())
		File(pendingPath()).renameTo(File(filepath))
	}

	/**
	 * Get a temporary filename when transferring.
	 *
	 * @return pending filename
	 */
	private fun pendingPath(): String {
		return Helper.resolvePath(dir, ".pending-" + uniqueFilename())
	}
}