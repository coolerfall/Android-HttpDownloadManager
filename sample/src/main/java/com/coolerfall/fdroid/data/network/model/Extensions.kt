package com.coolerfall.fdroid.data.network.model

import com.coolerfall.fdroid.app.Constant

/**
 * @author Vincent Cheung (coolingfall@gmail.com)
 */

typealias LocalizedFile = Map<String, File>

typealias LocalizedText = Map<String, String>

typealias MultipleVersion = Map<String, Version?>

fun LocalizedFile.url(locale: String = "en-US"): String? {
	val file = this[locale] ?: return null
	return Constant.BASE_URL + "repo" + file.name
}

fun LocalizedText.text(locale: String = "en-US"): String? {
	return this[locale]
}

fun File.url(): String {
	return Constant.BASE_URL + "repo" + name
}

fun MultipleVersion.firstUrl(): String? {
	return values.filterNotNull().first().file?.url()
}
