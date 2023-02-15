package com.coolerfall.fdroid.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coolerfall.fdroid.data.model.AppInfo
import com.coolerfall.fdroid.data.repository.MarketRepository
import com.coolerfall.fdroid.ext.autoLaunch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber
import javax.inject.Inject

/**
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
@HiltViewModel
class AppInfoViewModel @Inject constructor(
	private val marketRepository: MarketRepository
) : ViewModel() {

	private val isLoading = MutableStateFlow(true)
	private val packages = MutableStateFlow<List<AppInfo>>(emptyList())

	val appInfoState = combine(isLoading, packages) { isLoading, appInfos ->
		AppInfoState(loading = isLoading, appInfos = appInfos)
	}.stateIn(
		viewModelScope,
		started = SharingStarted.WhileSubscribed(),
		initialValue = AppInfoState()
	)

	fun loadApps() {
		isLoading.value = true
		marketRepository.loadPackages()
			.onEach { packages.value = it }
			.onCompletion { isLoading.value = false }
			.catch { Timber.e(it) }
			.autoLaunch(this)
	}
}