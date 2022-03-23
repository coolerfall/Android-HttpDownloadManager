package com.coolerfall.download

import android.net.Uri
import com.coolerfall.download.DownloadState.PENDING
import com.coolerfall.download.Helper.resolvePath
import com.coolerfall.download.Preconditions.checkNotNull
import com.coolerfall.download.Priority.NORMAL
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.Int.Companion

/**
 * This class represents a request for downloading which contains necessary configuration.
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
class DownloadRequest private constructor(builder: Builder) : Comparable<DownloadRequest> {
  private var downloadId: Int
  private val retryTime: AtomicInteger
  private var downloadState: DownloadState
  private val uri: Uri
  private val relativeDirectory: String?
  private val relativeFilepath: String?
  private var destinationFilepath: String? = null
  private lateinit var destinationDirectory: String
  private val progressInterval: Long
  private val retryInterval: Long
  private var downloadRequestQueue: DownloadRequestQueue? = null
  private val timestamp: Long
  private val priority: Priority
  private var downloader: Downloader? = null
  private val downloadCallback: DownloadCallback

  init {
    downloadId = builder.downloadId
    uri = checkNotNull(builder.uri, "uri == null")
    priority = checkNotNull(builder.priority, "priority == null")
    retryTime = AtomicInteger(builder.retryTime)
    relativeDirectory = builder.relativeDirectory
    relativeFilepath = builder.relativeFilepath
    downloadCallback = checkNotNull(builder.downloadCallback, "downloadCallback == null")
    progressInterval = builder.progressInterval
    retryInterval = builder.retryInterval
    downloadState = PENDING
    timestamp = System.currentTimeMillis()
  }

  /**
   * To check if current request has canceled.
   *
   * @return Returns true if this request has been canceled.
   */
  internal var isCanceled = false
    private set

  override fun compareTo(other: DownloadRequest): Int {
    val left = priority()
    val right = other.priority()

    /*
     * High-priority requests are "lesser" so they are sorted to the front.
     * Equal priorities are sorted by timestamp to provide FIFO ordering.
     */
    return if (left === right)
      (timestamp - other.timestamp).toInt()
    else right.ordinal - left.ordinal
  }

  /**
   * Get the priority of download request.
   *
   * @return [Priority.NORMAL] by default.
   */
  private fun priority(): Priority {
    return priority
  }

  /**
   * Get [Downloader] to use.
   *
   * @return [Downloader]
   */
  internal fun downloader(): Downloader? {
    return downloader
  }

  /**
   * Set a downloader for current reqeust to use.
   *
   * @param downloader [Downloader]
   */
  internal fun downloader(downloader: Downloader) {
    this.downloader = downloader
  }

  /**
   * Get the download callback of this request.
   *
   * @return download callback
   */
  internal fun downloadCallback(): DownloadCallback {
    return downloadCallback
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
   * Update the [DownloadState] of current download request.
   *
   * @param downloadState [DownloadState]
   */
  fun updateDownloadState(downloadState: DownloadState) {
    this.downloadState = downloadState
  }

  /**
   * Get download state of current request.
   *
   * @return download state
   */
  internal fun downloadState(): DownloadState {
    return downloadState
  }

  /**
   * Get the download id of this download request.
   *
   * @return download id
   */
  internal fun downloadId(): Int {
    return downloadId
  }

  /**
   * Get retry time, the retry time will decrease automatically after invoking this method.
   *
   * @return retry time
   */
  internal fun retryTime(): Int {
    return retryTime.decrementAndGet()
  }

  /**
   * Get progress interval, used in [DownloadDispatcher].
   *
   * @return progress interval
   */
  internal fun progressInterval(): Long {
    return progressInterval
  }

  /**
   * Get retry interval, used in [DownloadDispatcher].
   *
   * @return retry interval
   */
  internal fun retryInterval(): Long {
    return retryInterval
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
  fun tempFilepath(): String {
    return destinationFilepath() + ".tmp"
  }

  /**
   * Mark this download request as canceled. No callback will be delivered.
   */
  internal fun cancel() {
    isCanceled = true
  }

  /**
   * Notifies the download request queue that this request has finished(succesfully or fail)
   */
  fun finish() {
    downloadRequestQueue?.finish(this)
  }

  class Builder {
    internal var downloadId = -1
    internal var uri: Uri? = null
    internal var retryTime = 1
    internal var retryInterval: Long = 3000
    internal var relativeDirectory: String? = null
    internal var relativeFilepath: String? = null
    internal var priority: Priority
    internal var progressInterval: Long = 100
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
      this.uri = checkNotNull(uri, "uri == null")
      val scheme = uri.scheme
      require(
          !(Helper.HTTP != scheme && Helper.HTTPS != scheme)
      ) { "url should start with http or https" }
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

    fun retryTime(retryTime: Int): Builder {
      require(retryTime >= 0) { "retryTime < 0" }
      this.retryTime = retryTime
      return this
    }

    fun retryInterval(
      interval: Long,
      unit: TimeUnit
    ): Builder {
      require(interval > 0) { "interval <= 0" }
      checkNotNull(unit, "unit == null")
      val millis = unit.toMillis(interval)
      require(millis <= Int.MAX_VALUE) { "interval too large" }
      retryInterval = millis
      return this
    }

    fun progressInterval(
      interval: Long,
      unit: TimeUnit
    ): Builder {
      require(interval >= 0) { "interval < 0" }
      checkNotNull(unit, "unit == null")
      val millis = unit.toMillis(interval)
      require(millis <= Companion.MAX_VALUE) { "interval too large" }
      progressInterval = millis
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