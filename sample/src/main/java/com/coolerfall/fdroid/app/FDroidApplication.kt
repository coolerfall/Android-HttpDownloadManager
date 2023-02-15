package com.coolerfall.fdroid.app

import android.app.Application
import com.coolerfall.fdroid.BuildConfig
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import timber.log.Timber.DebugTree

/**
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
@HiltAndroidApp
class FDroidApplication : Application() {

	override fun onCreate() {
		super.onCreate()

		if (BuildConfig.DEBUG) {
			Timber.plant(DebugTree())
		}
	}
}