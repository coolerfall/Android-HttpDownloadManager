package com.coolerfall.fdroid.data.network.api

import com.coolerfall.fdroid.data.network.model.Entry
import com.coolerfall.fdroid.data.network.model.Index
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
interface FDroidApi {

	@GET("repo/entry.json")
	suspend fun loadEntry(): Entry

	@GET("repo/{name}")
	suspend fun loadIndex(@Path("name", encoded = true) name: String): Index
}