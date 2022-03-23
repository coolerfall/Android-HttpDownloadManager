package com.coolerfall.download

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.coolerfall.download.DownloadState.INVALID
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicInteger

/**
 * Create the download dispatchers according to pool size. Any number higher than 10 or less
 * than 1, then the size will be default size.
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
class DownloadRequestQueue(
  threadPoolSize: Int,
  private val logger: Logger
) {

  companion object {
    private val TAG = DownloadRequestQueue::class.java.simpleName
    private const val CAPACITY = 20
    private const val DEFAULT_DOWNLOAD_THREAD_POOL_SIZE = 3
  }

  /**
   * The set of all requests currently being processed by this DownloadQueue. A Request
   * will be in this set if it is waiting in any queue or currently being processed by
   * any dispatcher.
   */
  private val currentRequests: MutableSet<DownloadRequest> = HashSet()
  private var downloadQueue: PriorityBlockingQueue<DownloadRequest> =
    PriorityBlockingQueue(CAPACITY)
  private val dispatchers: Array<DownloadDispatcher?> = arrayOfNulls(
      if (threadPoolSize < 1 || threadPoolSize > 10)
        DEFAULT_DOWNLOAD_THREAD_POOL_SIZE else threadPoolSize
  )
  private val delivery: DownloadDelivery = DownloadDelivery(Handler(Looper.getMainLooper()))
  private val sequenceGenerator = AtomicInteger()

  /**
   * Starts the dispatchers in this queue.
   */
  fun start() {
    /* make sure any currently running dispatchers are stopped */
    stop()

    /* create the download dispatcher and start it. */
    for (i in dispatchers.indices) {
      val dispatcher = DownloadDispatcher(downloadQueue, delivery, logger)
      dispatchers[i] = dispatcher
      dispatcher.start()
    }
    logger.log("Thread pool size: " + dispatchers.size)
  }

  /**
   * Stops the download dispatchers.
   */
  private fun stop() {
    for (dispatcher in dispatchers) {
      dispatcher?.quit()
    }
  }

  /**
   * Add download request to the download request queue.
   *
   * @param request download request
   * @return true if the request is not in queue, otherwise return false
   */
  fun add(request: DownloadRequest): Boolean {
    /* if the request is downloading, do nothing */
    if (query(request.downloadId()) !== INVALID
        || query(request.uri()) !== INVALID
    ) {
      Log.w(TAG, "the download requst is in downloading")
      return false
    }

    /* tag the request as belonging to this queue */
    request.downloadRequestQueue(this)
    /* add it to the set of current requests */
    synchronized(currentRequests) {
      currentRequests.add(
          request
      )
    }

    /* process requests in the order they are added in */
    downloadQueue.add(request)
    return true
  }

  /**
   * Cancel a download in progress.
   *
   * @param downloadId download id
   * @return true if download has canceled, otherwise return false
   */
  fun cancel(downloadId: Int): Boolean {
    synchronized(currentRequests) {
      for (request in currentRequests) {
        if (request.downloadId() != downloadId) {
          continue
        }

        request.cancel()
        currentRequests.remove(request)
        return true
      }
    }
    return false
  }

  /**
   * Cancel all the download.
   */
  fun cancelAll() {
    synchronized(currentRequests) {
      for (request in currentRequests) {
        request.cancel()
      }
    }
    currentRequests.clear()
  }

  /**
   * Get the downloading task size.
   *
   * @return task size
   */
  val downloadingSize: Int
    get() = currentRequests.size

  /**
   * To check if the request is downloading according to download id.
   *
   * @param downloadId download id
   * @return true if the request is downloading, otherwise return false
   */
  fun query(downloadId: Int): DownloadState {
    synchronized(currentRequests) {
      for (request in currentRequests) {
        if (request.downloadId() == downloadId) {
          return request.downloadState()
        }
      }
    }
    return INVALID
  }

  /**
   * To check if the request is downloading according to download url.
   *
   * @param uri the uri to check
   * @return true if the request is downloading, otherwise return false
   */
  fun query(uri: Uri): DownloadState {
    synchronized(currentRequests) {
      for (request in currentRequests) {
        if (request.uri()
                .toString() == uri.toString()
        ) {
          return request.downloadState()
        }
      }
    }
    return INVALID
  }

  /**
   * Gets a sequence number.
   *
   * @return return the sequence number
   */
  val sequenceNumber: Int
    get() = sequenceGenerator.incrementAndGet()

  /**
   * The download has finished and remove from set.
   *
   * @param request download reqeust
   */
  fun finish(request: DownloadRequest) {
    synchronized(currentRequests) { currentRequests.remove(request) }
  }

  /**
   * Release all the resource.
   */
  fun release() {
    /* release current download request */
    cancelAll()

    /* release download queue */
    downloadQueue.clear()

    /* release dispathcers */
    stop()
    dispatchers.fill(null)
  }
}