package com.coolerfall.fdroid.data.network.datasource

import com.coolerfall.fdroid.data.model.AppInfo
import com.coolerfall.fdroid.data.network.api.FDroidApi
import com.coolerfall.fdroid.data.network.model.firstUrl
import com.coolerfall.fdroid.data.network.model.text
import com.coolerfall.fdroid.data.network.model.url
import com.coolerfall.fdroid.data.repository.MarketRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
@OptIn(FlowPreview::class)
@Singleton
class MarketDataSource @Inject constructor(retrofit: Retrofit) : MarketRepository {

	private val fDroidApi: FDroidApi = retrofit.create(FDroidApi::class.java)

	override fun loadPackages(): Flow<List<AppInfo>> {
		return flow { emit(fDroidApi.loadEntry()) }
			.filter { it.diffs.isNotEmpty() }
			.flatMapConcat {
				val file = it.diffs.entries.first().value
				flow {
					emit(fDroidApi.loadIndex(file.name))
				}
			}.map {
				it.packages.values.asSequence().filterNotNull()
					.filter { pkg -> pkg.metadata?.name?.text() != null }
					.filter { pkg -> pkg.metadata?.summary?.text() != null }
					.filter { pkg -> pkg.metadata?.icon?.url() != null }
					.filter { pkg -> pkg.versions.firstUrl() != null }
					.take(20).map { pkg ->
						AppInfo(
							icon = pkg.metadata!!.icon!!.url()!!,
							name = pkg.metadata.name!!.text()!!,
							summary = pkg.metadata.summary!!.text()!!,
							apkUrl = pkg.versions.firstUrl()!!
						)
					}.toList()
			}
	}
}