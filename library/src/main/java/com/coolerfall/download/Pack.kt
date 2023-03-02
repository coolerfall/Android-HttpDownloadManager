package com.coolerfall.download

import java.io.OutputStream

/**
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
abstract class Pack(private var filename: String? = null) {

	/**
	 * Open output stream for the given file in this pack.
	 *
	 * @return [OutputStream]
	 */
	internal abstract fun open(): OutputStream

	/**
	 * This pack is finished, move pending filename to real filename.
	 */
	internal abstract fun finish()

	/**
	 * Get the filename set in this pack.
	 *
	 * @return the target filename
	 * @throws [IllegalArgumentException] if filename is null
	 */
	protected fun requireFilename(): String {
		return requireNotNull(filename) { "filename == null" }
	}

	/**
	 * Put filename if absent.
	 */
	internal open fun putFilenameIfAbsent(filename: () -> String) {
		if (this.filename != null) {
			return
		}

		this.filename = filename()
	}
}