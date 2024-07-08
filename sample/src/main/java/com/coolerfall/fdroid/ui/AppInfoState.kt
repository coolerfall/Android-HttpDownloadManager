package com.coolerfall.fdroid.ui

import com.coolerfall.fdroid.data.model.AppInfo

/**
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
data class AppInfoState(
	val loading: Boolean = false,
	val appInfos: List<AppInfo> = emptyList()
)