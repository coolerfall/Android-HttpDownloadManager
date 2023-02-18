package com.coolerfall.fdroid.data.network.model

/**
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
data class Index(
	val repo: Repo,
	val packages: Map<String, AppPackage?> = emptyMap(),
)