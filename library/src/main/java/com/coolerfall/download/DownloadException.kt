package com.coolerfall.download

import java.io.IOException

/**
 * An exception represent an error for download.
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
class DownloadException internal constructor(
	val code: Int,
	detailMessage: String
) : IOException(detailMessage)