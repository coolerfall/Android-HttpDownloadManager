package com.coolerfall.fdroid.data.repository

import com.coolerfall.fdroid.data.model.AppInfo
import kotlinx.coroutines.flow.Flow

/**
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
interface MarketRepository {

	fun loadPackages(): Flow<List<AppInfo>>
}