package com.coolerfall.fdroid.data.network.model

/**
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
data class EntryFile(
	val name: String,
	val sha256: String? = null,
	val size: Long = 0,
	val numPackages: Int = 0
)