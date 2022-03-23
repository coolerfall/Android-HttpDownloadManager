package com.coolerfall.download

import android.os.Process
import com.coolerfall.download.DownloadState.FAILURE
import com.coolerfall.download.DownloadState.RUNNING
import com.coolerfall.download.DownloadState.SUCCESSFUL
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile
import java.util.concurrent.BlockingQueue

/**
 * This class used to dispatch downloader which will dispatch the download task.
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
internal class DownloadDispatcher(
  private val queue: BlockingQueue<DownloadRequest>,
  private val delivery: DownloadDelivery,
  private val logger: Logger
) : Thread() {

  private var lastProgressTimestamp: Long = 0

  @Volatile
  private var quit = false

  companion object {
    private const val BUFFER_SIZE = 4096
    private const val END_OF_STREAM = -1
    private const val DEFAULT_THREAD_NAME = "DownloadDispatcher"
    private const val IDLE_THREAD_NAME = "DownloadDispatcher-Idle"
  }

  init {
    /* set thread name to idle */
    name = IDLE_THREAD_NAME
  }

  override fun run() {
    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
    var request: DownloadRequest? = null
    while (true) {
      try {
        name = IDLE_THREAD_NAME
        request = queue.take()
        logger.log("A new download request taken, download id: " + request.downloadId())
        name = DEFAULT_THREAD_NAME

        /* start download */
        executeDownload(request)
      } catch (e: InterruptedException) {
        /* we may have been interrupted because it was time to quit */
        if (quit) {
          request?.finish()
          return
        }
      }
    }
  }

  /* update download state */
  private fun updateState(
    request: DownloadRequest?,
    state: DownloadState
  ) {
    request?.updateDownloadState(state)
  }

  /* update download start state */
  private fun updateStart(
    request: DownloadRequest,
    totalBytes: Long
  ) {
    /* if the request has failed before, donnot deliver callback */
    if (request.downloadState() === FAILURE) {
      updateState(request, RUNNING)
      return
    }

    /* set the download state of this request as running */
    updateState(request, RUNNING)
    delivery.postStart(request, totalBytes)
  }

  /* update download retrying */
  private fun updateRetry(request: DownloadRequest) {
    delivery.postRetry(request)
  }

  /* update download progress */
  private fun updateProgress(
    request: DownloadRequest,
    bytesWritten: Long,
    totalBytes: Long
  ) {
    val currentTimestamp = System.currentTimeMillis()
    if (bytesWritten != totalBytes
        && currentTimestamp - lastProgressTimestamp < request.progressInterval()
    ) {
      return
    }

    /* save progress timestamp */
    lastProgressTimestamp = currentTimestamp
    if (!request.isCanceled) {
      delivery.postProgress(request, bytesWritten, totalBytes)
    }
  }

  /* update download success */
  private fun updateSuccess(
    request: DownloadRequest
  ) {
    updateState(request, SUCCESSFUL)

    /* notify the request download finish */
    request.finish()
    val file = File(request.tempFilepath())
    if (file.exists()) {
      file.renameTo(File(request.destinationFilepath()))
    }

    /* deliver success message */
    delivery.postSuccess(request)
  }

  /* update download failure */
  private fun updateFailure(
    request: DownloadRequest,
    statusCode: Int,
    errMsg: String?
  ) {
    updateState(request, FAILURE)

    /* if the status code is 0, may be cause by the net error */
    val leftRetryTime = request.retryTime()
    if (leftRetryTime >= 0) {
      try {
        /* sleep a while before retrying */
        sleep(request.retryInterval())
      } catch (e: InterruptedException) {
        /* we may have been interrupted because it was time to quit */
        if (quit) {
          request.finish()
          return
        }
      }

      /* retry downloading */
      if (!request.isCanceled) {
        logger.log("Retry DownloadRequest: ${request.downloadId()} left retry time: $leftRetryTime")
        updateRetry(request)
        executeDownload(request)
      }
      return
    }

    /* notify the request that downloading has finished */
    request.finish()

    /* deliver failure message */
    delivery.postFailure(request, statusCode, errMsg)
  }

  /* execute downloading */
  private fun executeDownload(request: DownloadRequest) {
    if (currentThread().isInterrupted) {
      return
    }
    val downloader = request.downloader()!!
    var raf: RandomAccessFile? = null
    var `is`: InputStream? = null
    try {
      request.updateDestinationFilepath(downloader.detectFilename(request.uri()))
      val file = File(request.tempFilepath())
      val fileExsits = file.exists()
      raf = RandomAccessFile(file, "rw")
      val breakpoint = file.length()
      var bytesWritten: Long = 0
      if (fileExsits) {
        /* set the range to continue the downloading */
        raf.seek(breakpoint)
        bytesWritten = breakpoint
        logger.log("Detect existed file with $breakpoint bytes, start breakpoint downloading")
      }
      val statusCode = downloader.start(request.uri(), breakpoint)
      `is` = downloader.byteStream()
      if (statusCode != Helper.HTTP_OK && statusCode != Helper.HTTP_PARTIAL) {
        logger.log("Incorrect http code got: $statusCode")
        throw DownloadException(statusCode, "download fail")
      }
      var contentLength = downloader.contentLength()
      if (contentLength <= 0 && `is` == null) {
        throw DownloadException(statusCode, "content length error")
      }
      val noContentLength = contentLength <= 0
      contentLength += bytesWritten
      updateStart(request, contentLength)
      logger.log("Start to download, content length: $contentLength bytes")
      if (`is` != null) {
        val buffer = ByteArray(BUFFER_SIZE)
        var length: Int
        while (true) {
          /* if the request has canceld, stop the downloading */
          if (currentThread().isInterrupted || request.isCanceled) {
            request.finish()
            return
          }

          /* read data into buffer from input stream */
          length = readFromInputStream(buffer, `is`)
          val fileSize = raf.length()
          val totalBytes = if (noContentLength) fileSize else contentLength
          if (length == END_OF_STREAM) {
            updateSuccess(request)
            return
          } else if (length == Int.MIN_VALUE) {
            throw DownloadException(statusCode, "transfer data error")
          }
          bytesWritten += length.toLong()
          /* write buffer into local file */
          raf.write(buffer, 0, length)

          /* deliver progress callback */
          updateProgress(request, bytesWritten, totalBytes)
        }
      } else {
        throw DownloadException(statusCode, "input stream error")
      }
    } catch (e: IOException) {
      logger.log("Caught new exception: " + e.message)
      if (e is DownloadException) {
        updateFailure(request, e.code, e.message)
      } else {
        updateFailure(request, 0, e.message)
      }
    } finally {
      downloader.close()
      silentCloseFile(raf)
      silentCloseInputStream(`is`)
    }
  }

  /* read data from input stream */
  private fun readFromInputStream(
    buffer: ByteArray?,
    `is`: InputStream
  ): Int {
    return try {
      `is`.read(buffer)
    } catch (e: IOException) {
      logger.log("Transfer data with exception: " + e.message)
      Int.MIN_VALUE
    }
  }

  /**
   * Forces this dispatcher to quit immediately. If any download requests are still in
   * the queue, they are not guaranteed to be processed.
   */
  fun quit() {
    logger.log("Download dispatcher quit")
    quit = true

    /* interrupt current thread */
    interrupt()
  }

  /* a utility function to close a random access file without raising an exception */
  private fun silentCloseFile(raf: RandomAccessFile?) {
    if (raf != null) {
      try {
        raf.close()
      } catch (ignore: IOException) {
      }
    }
  }

  /* a utility function to close an input stream without raising an exception */
  private fun silentCloseInputStream(`is`: InputStream?) {
    try {
      `is`?.close()
    } catch (ignore: IOException) {
    }
  }
}