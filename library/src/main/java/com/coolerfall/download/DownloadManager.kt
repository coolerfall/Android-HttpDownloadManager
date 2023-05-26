package com.coolerfall.download

import android.net.Uri
import com.coolerfall.download.DownloadState.INVALID
import com.coolerfall.download.Helper.createDefaultDownloader
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * A manager used to manage the downloading.
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
class DownloadManager internal constructor(builder: Builder) {
	private val downloader: Downloader = requireNotNull(builder.downloader) { "downloader == null" }
	private val threadPoolSize: Int = builder.threadPoolSize
	private val logger: Logger = builder.logger
	private val downloadRequestQueue = DownloadRequestQueue(threadPoolSize, logger)
	private val progressIntervalMs: Long = builder.progressIntervalMs
	private val retryTime: Int = builder.retryTime
	private val retryIntervalMs: Long = builder.retryIntervalMs
	val taskSize: Int
		get() = downloadRequestQueue.downloadingSize

	init {
		downloadRequestQueue.start()
	}

	companion object {
		private var downloadManager: DownloadManager? = null
		private var builder: Builder? = null

		@JvmStatic
		@Synchronized
		fun get(): DownloadManager {
			/* check again in case download manager was just set */
			downloadManager?.let { return it }

			val newDownloadManager = (builder ?: Builder()).build()
			downloadManager = newDownloadManager

			return newDownloadManager
		}

		@JvmStatic
		fun withBuilder(builder: Builder) {
			this.builder = builder
		}
	}

	@Deprecated(
		level = DeprecationLevel.ERROR,
		message = "Use enqueue",
		replaceWith = ReplaceWith("enqueue(request)")
	)
	fun add(request: DownloadRequest): Int {
		return 0
	}

	/**
	 * Add one download request into the queue.
	 *
	 * @param request download request
	 * @return download id, if the id is not set, then manager will generate one.
	 * if the request is in downloading, then -1 will be returned
	 */
	fun enqueue(request: DownloadRequest): Int {
		if (isDownloading(request.uri.toString())) {
			return -1
		}
		request.progressInterval = progressIntervalMs
		request.retryTime = AtomicInteger(retryTime)
		request.retryInterval = retryIntervalMs
		request.downloader(downloader.copy())

		/* add download request into download request queue */
		return if (downloadRequestQueue.add(request)) request.downloadId else -1
	}

	/**
	 * Query download from download request queue.
	 *
	 * @param downloadId download id
	 * @return download state
	 */
	fun query(downloadId: Int): DownloadState {
		return downloadRequestQueue.query(downloadId)
	}

	/**
	 * Query download from download request queue.
	 *
	 * @param uri download url
	 * @return download state
	 */
	fun query(uri: Uri): DownloadState {
		return downloadRequestQueue.query(uri)
	}

	/**
	 * To check if the download was in the request queue.
	 *
	 * @param downloadId download id
	 * @return true if was downloading, otherwise return false
	 */
	fun isDownloading(downloadId: Int): Boolean {
		return query(downloadId) !== INVALID
	}

	/**
	 * To check if the download was in the request queue.
	 *
	 * @param url download url
	 * @return true if was downloading, otherwise return false
	 */
	fun isDownloading(url: String): Boolean {
		return query(Uri.parse(url)) !== INVALID
	}

	/**
	 * To check if the download was in the request queue.
	 *
	 * @param uri download uri
	 * @return true if was downloading, otherwise return false
	 */
	fun isDownloading(uri: Uri): Boolean {
		return query(uri) !== INVALID
	}

	/**
	 * Cancel the download according to download id.
	 *
	 * @param downloadId download id
	 * @return true if download has canceled, otherwise return false
	 */
	fun cancel(downloadId: Int): Boolean {
		return downloadRequestQueue.cancel(downloadId)
	}

	/**
	 * Cancel the download according to download id.
	 *
	 * @param request download download
	 * @return true if download has canceled, otherwise return false
	 */
	fun cancel(request: DownloadRequest): Boolean {
		return downloadRequestQueue.cancel(request.downloadId)
	}

	/**
	 * Cancel all the downloading in queue.
	 */
	fun cancelAll() {
		downloadRequestQueue.cancelAll()
	}

	/**
	 * Release all the resource.
	 */
	fun release() {
		downloadRequestQueue.release()
	}

	/**
	 * Copy downloaded file to external public download directory.
	 *
	 * @param filepath filepath of downloaded file
	 * @return true if copy successfully, otherwise return false
	 */
	@Deprecated(
		level = DeprecationLevel.ERROR,
		message = "Use pack instead",
		replaceWith = ReplaceWith("request.target(pack)")
	)
	fun copyToPublicDownloadDir(filepath: String): Boolean {
		return false
	}

	fun newBuilder(): Builder {
		return Builder(this)
	}

	class Builder {
		internal var downloader: Downloader? = null
		internal var threadPoolSize: Int
		internal var logger: Logger
		internal var retryTime = 1
		internal var retryIntervalMs: Long = 3000
		internal var progressIntervalMs: Long = 500

		constructor() {
			threadPoolSize = 3
			logger = Logger.EMPTY
		}

		internal constructor(downloadManager: DownloadManager) {
			downloader = downloadManager.downloader
			threadPoolSize = downloadManager.threadPoolSize
			logger = downloadManager.logger
		}

		fun downloader(downloader: Downloader): Builder {
			this.downloader = downloader
			return this
		}

		fun threadPoolSize(threadPoolSize: Int): Builder {
			this.threadPoolSize = threadPoolSize
			return this
		}

		fun logger(logger: Logger): Builder {
			this.logger = logger
			return this
		}

		fun retryTime(retryTime: Int): Builder {
			require(retryTime >= 0) { "retryTime < 0" }
			this.retryTime = retryTime
			return this
		}

		fun retryInterval(interval: Long, unit: TimeUnit): Builder {
			require(interval > 0) { "interval <= 0" }
			val millis = unit.toMillis(interval)
			require(millis <= Int.MAX_VALUE) { "interval too large" }
			retryIntervalMs = millis
			return this
		}

		fun progressInterval(interval: Long, unit: TimeUnit): Builder {
			require(interval >= 0) { "interval < 0" }
			val millis = unit.toMillis(interval)
			require(millis <= Int.MAX_VALUE) { "interval too large" }
			progressIntervalMs = millis
			return this
		}

		fun build(): DownloadManager {
			if (downloader == null) {
				downloader = createDefaultDownloader()
			}
			return DownloadManager(this)
		}
	}
}