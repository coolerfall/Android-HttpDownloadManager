package com.coolerfall.fdroid.ext

import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn

/**
 * @author Vincent Cheung (coolingfall@gmail.com)
 */

fun <T> Flow<T>.launchOnCreated(fragment: Fragment): Job {
	return this
		.flowWithLifecycle(fragment.viewLifecycleOwner.lifecycle, Lifecycle.State.CREATED)
		.launchIn(fragment.lifecycleScope)
}

fun <T> Flow<T>.launchOnStarted(fragment: Fragment): Job {
	return this
		.flowWithLifecycle(fragment.viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
		.launchIn(fragment.lifecycleScope)
}

fun <T> Flow<T>.launchOnResumed(fragment: Fragment): Job {
	return this
		.flowWithLifecycle(fragment.viewLifecycleOwner.lifecycle, Lifecycle.State.RESUMED)
		.launchIn(fragment.lifecycleScope)
}

fun <T> Flow<T>.launchOnCreated(activity: ComponentActivity): Job {
	return this
		.flowWithLifecycle(activity.lifecycle, Lifecycle.State.CREATED)
		.launchIn(activity.lifecycleScope)
}

fun <T> Flow<T>.launchOnStarted(activity: ComponentActivity): Job {
	return this
		.flowWithLifecycle(activity.lifecycle, Lifecycle.State.STARTED)
		.launchIn(activity.lifecycleScope)
}

fun <T> Flow<T>.launchOnResumed(activity: ComponentActivity): Job {
	return this
		.flowWithLifecycle(activity.lifecycle, Lifecycle.State.RESUMED)
		.launchIn(activity.lifecycleScope)
}

fun <T> Flow<T>.autoLaunch(viewModel: ViewModel): Job {
	return this
		.launchIn(viewModel.viewModelScope)
}