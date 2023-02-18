package com.coolerfall.fdroid.data.network.model

/**
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
data class File(
	val name: String,
	val sha256: String? = null,
	val size: Long = 0,
)
