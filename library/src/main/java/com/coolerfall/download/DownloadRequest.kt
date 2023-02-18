package com.coolerfall.download

import android.net.Uri
import com.coolerfall.download.DownloadState.PENDING
import com.coolerfall.download.Helper.resolvePath
import com.coolerfall.download.Priority.NORMAL
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.properties.Delegates

/**
 * This class represents a request for downloading which contains necessary configuration.
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
class DownloadRequest private constructor(builder: Builder) : Comparable<DownloadRequest> {
	private val uri: Uri
	private val relativeDirectory: String?
	private val relativeFilepath: String?
	private var destinationFilepath: String? = null
	private lateinit var destinationDirectory: String
	private var downloadRequestQueue: DownloadRequestQueue? = null
	private val timestamp: Long
	private val priority: Priority
	lateinit var downloader: Downloader
		private set
	internal val downloadCallback: DownloadCallback
	internal var downloadId: Int
		private set
	internal var downloadState: DownloadState
	internal var progressInterval by Delegates.notNull<Long>()
	internal lateinit var retryTime: AtomicInteger
	internal var retryInterval by Delegates.notNull<Long>()
	internal var isCanceled = false
		private set

	init {
		downloadId = builder.downloadId
		uri = requireNotNull(builder.uri) { "uri == null" }
		priority = requireNotNull(builder.priority) { "priority == null" }
		relativeDirectory = builder.relativeDirectory
		relativeFilepath = builder.relativeFilepath
		downloadCallback = requireNotNull(builder.downloadCallback) { "downloadCallback == null" }
		downloadState = PENDING
		timestamp = System.currentTimeMillis()
	}

	override fun compareTo(other: DownloadRequest): Int {
		val left = priority
		val right = other.priority

		/*
		 * High-priority requests are "lesser" so they are sorted to the front.
		 * Equal priorities are sorted by timestamp to provide FIFO ordering.
		 */
		return if (left === right)
			(timestamp - other.timestamp).toInt()
		else right.ordinal - left.ordinal
	}

	/**
	 * Set a downloader for current request to use.
	 *
	 * @param downloader [Downloader]
	 */
	internal fun downloader(downloader: Downloader) {
		this.downloader = downloader
	}

	/**
	 * Associates this request with the given queue. The request queue will be
	 * notified when this request has finished.
	 *
	 * @param queue download request queue
	 */
	internal fun downloadRequestQueue(queue: DownloadRequestQueue) {
		downloadRequestQueue = queue
		if (downloadId < 0) {
			downloadId = downloadRequestQueue!!.sequenceNumber
		}
	}

	/**
	 * Config root download directory of current app, called by [DownloadManager].
	 *
	 * @param rootDownloadDir root download dir
	 */
	fun rootDownloadDir(rootDownloadDir: String) {
		destinationDirectory =
			relativeDirectory?.let { resolvePath(rootDownloadDir, it) } ?: rootDownloadDir
		if (relativeFilepath != null) {
			destinationFilepath = resolvePath(rootDownloadDir, relativeFilepath)
			require(!File(destinationFilepath!!).isDirectory) { "relativeFilepath cannot be a directory" }
		}
	}

	/**
	 * Get the URL of this request.
	 *
	 * @return the URL of this request
	 */
	fun uri(): Uri {
		return uri
	}

	/**
	 * Update absolute filepath according to the directory and filename.
	 *
	 * @param filename filename to save
	 */
	fun updateDestinationFilepath(filename: String) {
		if (destinationFilepath != null) {
			return
		}

		/* if the destination path is directory */
		File(resolvePath(destinationDirectory, filename)).let {
			if (!it.exists()) {
				/* make dirs in case */
				it.parentFile?.mkdirs()
			}
			destinationFilepath = it.toString()
		}
	}

	/**
	 * Get destination filepath of this download request.
	 *
	 * @return destination filepath
	 */
	fun destinationFilepath(): String {
		return destinationFilepath!!
	}

	/**
	 * Get temporary destination filepath of this download request.
	 *
	 * @return temporary destination filepath
	 */
	internal fun tempFilepath(): String {
		return destinationFilepath() + ".tmp"
	}

	/**
	 * Mark this download request as canceled. No callback will be delivered.
	 */
	internal fun cancel() {
		isCanceled = true
	}

	/**
	 * Notifies the download request queue that this request has finished(successfully or fail)
	 */
	fun finish() {
		downloadRequestQueue?.finish(this)
	}

	class Builder {
		internal var downloadId = -1
		internal var uri: Uri? = null
		internal var relativeDirectory: String? = null
		internal var relativeFilepath: String? = null
		internal var priority: Priority
		internal var downloadCallback: DownloadCallback

		init {
			priority = NORMAL
			downloadCallback = DownloadCallback.EMPTY_CALLBACK
		}

		fun downloadId(downloadId: Int): Builder {
			this.downloadId = downloadId
			return this
		}

		fun url(url: String): Builder {
			return uri(Uri.parse(url))
		}

		fun uri(uri: Uri): Builder {
			val scheme = uri.scheme
			require(
				!(Helper.HTTP != scheme && Helper.HTTPS != scheme)
			) { "url should start with http or https" }
			this.uri = uri

			return this
		}

		fun relativeDirectory(relativeDirectory: String): Builder {
			this.relativeDirectory = relativeDirectory
			return this
		}

		fun relativeFilepath(relativeFilepath: String): Builder {
			this.relativeFilepath = relativeFilepath
			return this
		}

		fun priority(priority: Priority): Builder {
			this.priority = priority
			return this
		}

		@Deprecated(
			level = DeprecationLevel.ERROR,
			message = "Moved to DownloadManager",
			replaceWith = ReplaceWith("downloadManger.retryTime(retryTime)")
		)
		fun retryTime(retryTime: Int): Builder {
			return this
		}

		@Deprecated(
			level = DeprecationLevel.ERROR,
			message = "Moved to DownloadManager",
			replaceWith = ReplaceWith("downloadManger.retryInterval(interval, unit)")
		)
		fun retryInterval(interval: Long, unit: TimeUnit): Builder {
			return this
		}

		@Deprecated(
			level = DeprecationLevel.ERROR,
			message = "Moved to DownloadManager",
			replaceWith = ReplaceWith("downloadManger.progressInterval(interval, unit)")
		)
		fun progressInterval(interval: Long, unit: TimeUnit): Builder {
			return this
		}

		fun downloadCallback(downloadCallback: DownloadCallback): Builder {
			this.downloadCallback = downloadCallback
			return this
		}

		fun build(): DownloadRequest {
			return DownloadRequest(this)
		}
	}
}