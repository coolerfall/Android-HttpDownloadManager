package com.coolerfall.fdroid.data.network.model

/**
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
data class Repo(
	val name: LocalizedText = emptyMap(),
	val icon: LocalizedFile = emptyMap(),
	val timestamp: Long,
)