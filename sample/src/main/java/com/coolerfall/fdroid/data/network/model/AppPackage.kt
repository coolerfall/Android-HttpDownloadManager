package com.coolerfall.fdroid.data.network.model

/**
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
data class AppPackage(
	val metadata: Metadata?,
	val versions: MultipleVersion = emptyMap(),
)