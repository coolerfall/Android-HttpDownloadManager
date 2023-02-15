package com.coolerfall.fdroid.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import com.coolerfall.download.DownloadManager
import com.coolerfall.fdroid.databinding.ActivityMainBinding
import com.coolerfall.fdroid.ext.launchOnCreated
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.onEach

/**
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

	private lateinit var binding: ActivityMainBinding
	private val viewModel: AppInfoViewModel by viewModels()
	private val appInfoAdapter = AppInfoAdapter()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityMainBinding.inflate(layoutInflater)
		setContentView(binding.root)

		with(binding.recyclerView) {
			adapter = appInfoAdapter
		}

		viewModel.appInfoState
			.onEach {
				if (it.loading) {
					binding.loadingIndicator.show()
				} else {
					binding.loadingIndicator.hide()
				}
				appInfoAdapter.addData(it.appInfos)
			}
			.launchOnCreated(this)

		viewModel.loadApps()
	}

	override fun onDestroy() {
		super.onDestroy()
		DownloadManager.get().cancelAll()
	}
}