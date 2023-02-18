package com.coolerfall.fdroid.data.network.model

/**
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
data class Entry(
	val timestamp: Long,
	val version: Long,
	val maxAge: Int? = null,
	val index: EntryFile,
	val diffs: Map<String, EntryFile> = emptyMap(),
)