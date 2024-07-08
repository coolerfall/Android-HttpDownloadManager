package com.coolerfall.fdroid.data.network.model

/**
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
data class Metadata(
	val name: LocalizedText? = null,
	val summary: LocalizedText? = null,
	val description: LocalizedText? = null,
	val lastUpdated: Long = 0,
	val webSite: String? = null,
	val icon: LocalizedFile? = null,
)