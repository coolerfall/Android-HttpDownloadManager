package com.coolerfall.download

import android.net.Uri
import com.coolerfall.download.DownloadState.PENDING
import com.coolerfall.download.Priority.NORMAL
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.properties.Delegates

/**
 * This class represents a request for downloading which contains necessary configuration.
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
class DownloadRequest private constructor(builder: Builder) : Comparable<DownloadRequest> {
	internal val uri: Uri = requireNotNull(builder.uri) { "uri == null" }
	internal val pack: Pack = requireNotNull(builder.pack) { "pack == null" }
	private var downloadRequestQueue: DownloadRequestQueue? = null
	private val timestamp: Long = System.currentTimeMillis()
	private val priority: Priority = requireNotNull(builder.priority) { "priority == null" }
	lateinit var downloader: Downloader
		private set
	internal val downloadCallback: DownloadCallback =
		requireNotNull(builder.downloadCallback) { "downloadCallback == null" }
	internal var downloadId: Int = builder.downloadId
	internal var downloadState: DownloadState = PENDING
	internal var progressInterval by Delegates.notNull<Long>()
	internal lateinit var retryTime: AtomicInteger
	internal var retryInterval by Delegates.notNull<Long>()
	internal var isCanceled = false
		private set

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
	 * Mark this download request as canceled. No callback will be delivered.
	 */
	internal fun cancel() {
		isCanceled = true
	}

	/**
	 * Notifies the download request queue that this request has finished(successfully or fail)
	 */
	fun finish() {
		pack.finish()
		downloadRequestQueue?.finish(this)
	}

	class Builder {
		internal var downloadId = -1
		internal var uri: Uri? = null
		internal var pack: Pack? = null
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

		@Deprecated(
			level = DeprecationLevel.ERROR,
			message = "Use pack instead",
			replaceWith = ReplaceWith("target(target)")
		)
		fun relativeDirectory(relativeDirectory: String): Builder {
			return this
		}

		@Deprecated(
			level = DeprecationLevel.ERROR,
			message = "Use pack instead",
			replaceWith = ReplaceWith("target(target)")
		)
		fun relativeFilepath(relativeFilepath: String): Builder {
			return this
		}

		fun target(pack: Pack): Builder {
			this.pack = pack
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