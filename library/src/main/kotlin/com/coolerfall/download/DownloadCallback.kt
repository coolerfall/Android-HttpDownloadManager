package com.coolerfall.download

/**
 * This is a callback to be invoked when downloading.
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
interface DownloadCallback {

  companion object {
    internal val EMPTY_CALLBACK: DownloadCallback = object : DownloadCallback {}
  }

  /**
   * Invoked when downloading is started.
   *
   * @param downloadId download id in download request queue
   * @param totalBytes total bytes of the file
   */
  fun onStart(
    downloadId: Int,
    totalBytes: Long
  ) {
  }

  /**
   * Invoked when download retrying.
   *
   * @param downloadId download id in download request queue
   */
  fun onRetry(downloadId: Int) {}

  /**
   * Invoked when downloading is in progress.
   *
   * @param downloadId download id in download request queue
   * @param bytesWritten the bytes has written to local disk
   * @param totalBytes total bytes of the file
   */
  fun onProgress(
    downloadId: Int,
    bytesWritten: Long,
    totalBytes: Long
  ) {
  }

  /**
   * Invoked when downloading successfully.
   *
   * @param downloadId download id in download request queue
   * @param filepath the filepath of downloaded file
   */
  fun onSuccess(
    downloadId: Int,
    filepath: String
  ) {
  }

  /**
   * Invoked when downloading failed.
   *
   * @param downloadId download id in download request queue
   * @param statusCode status code
   * @param errMsg error message
   */
  fun onFailure(
    downloadId: Int,
    statusCode: Int,
    errMsg: String?
  ) {
  }
}