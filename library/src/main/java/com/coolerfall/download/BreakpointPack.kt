package com.coolerfall.download

/**
 * This [Pack] represents a pack which supports breakpoint transfer.
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
abstract class BreakpointPack(filename: String?) : Pack(filename) {

	/**
	 * Get pending length if pending file exists.
	 *
	 * @return pending length
	 */
	internal abstract fun pendingLength(): Long

	/**
	 * Get a unique filename for pending file.
	 *
	 * @return unique filename
	 */
	internal fun uniqueFilename(): String {
		return Helper.md5(requireFilename()) + "-" + requireFilename()
	}
}