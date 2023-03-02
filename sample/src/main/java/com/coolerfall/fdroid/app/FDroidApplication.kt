package com.coolerfall.fdroid.app

import android.app.Application
import com.coolerfall.download.DownloadManager
import com.coolerfall.fdroid.BuildConfig
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import timber.log.Timber.DebugTree
import java.util.concurrent.TimeUnit.MICROSECONDS
import java.util.concurrent.TimeUnit.SECONDS

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

		DownloadManager.withBuilder(
			DownloadManager.Builder()
				.retryInterval(3, SECONDS)
				.progressInterval(100, MICROSECONDS)
				.logger { message -> Timber.i(message) }
		)
	}
}