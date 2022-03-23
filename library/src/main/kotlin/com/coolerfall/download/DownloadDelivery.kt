package com.coolerfall.download

import android.os.Handler
import java.util.concurrent.Executor
import java.lang.Runnable

/**
 * This class is used to delivery callback to call back in main thread.
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
internal class DownloadDelivery(handler: Handler) {

  private val downloadPoster: Executor = Executor { r: Runnable -> handler.post(r) }

  /**
   * Post download start event.
   *
   * @param request download request
   * @param totalBytes total bytes
   */
  fun postStart(
    request: DownloadRequest,
    totalBytes: Long
  ) {
    downloadPoster.execute {
      request.downloadCallback()
          .onStart(request.downloadId(), totalBytes)
    }
  }

  /**
   * Post download retry event.
   *
   * @param request download request
   */
  fun postRetry(request: DownloadRequest) {
    downloadPoster.execute {
      request.downloadCallback()
          .onRetry(request.downloadId())
    }
  }

  /**
   * Post download progress event.
   *
   * @param request download request
   * @param bytesWritten the bytes have written to file
   * @param totalBytes the total bytes of currnet file in downloading
   */
  fun postProgress(
    request: DownloadRequest,
    bytesWritten: Long,
    totalBytes: Long
  ) {
    downloadPoster.execute {
      request.downloadCallback()
          .onProgress(request.downloadId(), bytesWritten, totalBytes)
    }
  }

  /**
   * Post download success event.
   *
   * @param request download request
   */
  fun postSuccess(request: DownloadRequest) {
    downloadPoster.execute {
      request.downloadCallback()
          .onSuccess(request.downloadId(), request.destinationFilepath())
    }
  }

  /**
   * Post download failure event.
   *
   * @param request download request
   * @param statusCode status code
   * @param errMsg error message
   */
  fun postFailure(
    request: DownloadRequest,
    statusCode: Int,
    errMsg: String?
  ) {
    downloadPoster.execute {
      request.downloadCallback()
          .onFailure(request.downloadId(), statusCode, errMsg)
    }
  }
}