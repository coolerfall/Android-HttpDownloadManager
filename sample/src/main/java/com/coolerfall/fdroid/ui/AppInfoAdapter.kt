package com.coolerfall.fdroid.ui

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.coolerfall.download.DownloadCallback
import com.coolerfall.download.DownloadManager
import com.coolerfall.download.DownloadRequest
import com.coolerfall.download.DownloadState.PENDING
import com.coolerfall.download.DownloadState.RUNNING
import com.coolerfall.download.DownloadState.SUCCESSFUL
import com.coolerfall.download.ExtPublicPack
import com.coolerfall.download.Pack
import com.coolerfall.fdroid.R
import com.coolerfall.fdroid.data.model.AppInfo
import com.coolerfall.fdroid.databinding.ItemAppInfoBinding
import com.coolerfall.fdroid.ui.AppInfoAdapter.ViewHolder

/**
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
class AppInfoAdapter : RecyclerView.Adapter<ViewHolder>() {

	private val dataList: MutableList<AppInfo> = mutableListOf()
	private val downloadMap: MutableMap<Int, Int> = mutableMapOf()
	private val progressMap: MutableMap<Int, Int> = mutableMapOf()

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val binding =
			ItemAppInfoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
		return ViewHolder(binding)
	}

	override fun getItemCount(): Int {
		return dataList.size
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		val pkg = dataList[position]
		with(holder.binding) {
			appIcon.load(pkg.icon) {
				placeholder(R.mipmap.ic_launcher)
				error(R.mipmap.ic_launcher)
			}

			appName.text = pkg.name
			appSummary.text = pkg.summary
			holder.bindDownload()

			btnToggle.setOnClickListener {
				holder.toggle(pkg.apkUrl)
			}
		}
	}

	inner class ViewHolder(val binding: ItemAppInfoBinding) :
		RecyclerView.ViewHolder(binding.root) {

		fun bindDownload() {
			val position = bindingAdapterPosition
			binding.indicator.tag = position
			val id = downloadMap[position]
			if (id == null) {
				binding.indicator.hide()
				binding.btnToggle.setText(R.string.start)
				binding.btnToggle.visibility = View.VISIBLE
				return
			}

			binding.indicator.show()
			binding.btnToggle.setText(R.string.stop)
			when (DownloadManager.get().query(id)) {
				PENDING -> binding.indicator.isIndeterminate = true
				RUNNING -> binding.indicator.setProgressCompat(progressMap[position] ?: 0, false)
				SUCCESSFUL -> binding.btnToggle.visibility = View.INVISIBLE
				else -> binding.indicator.hide()
			}
		}

		fun toggle(url: String) {
			val position = bindingAdapterPosition
			downloadMap[position]?.let {
				DownloadManager.get().cancel(it)
				downloadMap.remove(position)
				binding.btnToggle.setText(R.string.start)
				binding.indicator.hide()
				return
			}
			val request = DownloadRequest.Builder()
				.url(url)
				.target(ExtPublicPack())
				.downloadCallback(object : DownloadCallback {
					override fun onStart(downloadId: Int, totalBytes: Long) {
						progressMap[position] = 0
					}

					override fun onProgress(
						downloadId: Int,
						bytesWritten: Long,
						totalBytes: Long
					) {
						if (binding.indicator.tag != position) {
							return
						}
						val progress = (bytesWritten * 100 / totalBytes).toInt()
						progressMap[position] = progress
						binding.indicator.setProgressCompat(progress, true)
					}

					override fun onSuccess(downloadId: Int, pack: Pack) {
						binding.indicator.hide()
						binding.btnToggle.visibility = View.INVISIBLE
					}

					override fun onFailure(downloadId: Int, statusCode: Int, errMsg: String?) {
						progressMap[position] = 0
						downloadMap.remove(position)
						binding.btnToggle.setText(R.string.start)
					}
				}).build()

			val id = DownloadManager.get().enqueue(request)
			downloadMap[position] = id
			binding.btnToggle.setText(R.string.stop)
			binding.indicator.show()
			binding.indicator.isIndeterminate = true
		}
	}

	@SuppressLint("NotifyDataSetChanged")
	fun addData(appInfoList: List<AppInfo>) {
		dataList.clear()
		dataList.addAll(appInfoList)
		notifyDataSetChanged()
	}
}