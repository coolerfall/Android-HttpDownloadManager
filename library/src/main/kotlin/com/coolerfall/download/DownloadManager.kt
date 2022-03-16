package com.coolerfall.download

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Environment
import android.provider.MediaStore.Downloads
import android.provider.MediaStore.Files.FileColumns
import android.webkit.MimeTypeMap
import com.coolerfall.download.DownloadState.INVALID
import com.coolerfall.download.Helper.copy
import com.coolerfall.download.Helper.createDefaultDownloader
import com.coolerfall.download.Helper.resolvePath
import com.coolerfall.download.Preconditions.checkNotNull
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

/**
 * A manager used to manage the downloading.
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
class DownloadManager internal constructor(builder: Builder) {
  private val context: Context
  private val downloader: Downloader
  private val threadPoolSize: Int
  private val logger: Logger
  private val downloadRequestQueue: DownloadRequestQueue
  private val rootDownloadDir: String

  init {
    context = checkNotNull(builder.context, "context == null").applicationContext
    downloader = checkNotNull(builder.downloader, "downloader == null")
    threadPoolSize = builder.threadPoolSize
    logger = builder.logger
    downloadRequestQueue = DownloadRequestQueue(threadPoolSize, logger)
    downloadRequestQueue.start()
    val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
    checkNotNull(downloadDir, "shared storage is not currently available")
    rootDownloadDir = downloadDir?.absolutePath ?: ""
  }

  /**
   * Add one download request into the queue.
   *
   * @param request download request
   * @return download id, if the id is not set, then manager will generate one.
   * if the request is in downloading, then -1 will be returned
   */
  fun add(request: DownloadRequest): Int {
    checkNotNull(request, "request == null")
    if (isDownloading(
            request.uri()
                .toString()
        )
    ) {
      return -1
    }
    request.rootDownloadDir(rootDownloadDir)
    request.downloader(downloader.copy())

    /* add download request into download request queue */
    return if (downloadRequestQueue.add(request)) request.downloadId() else -1
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
   * @param url download url
   * @return download state
   */
  fun query(url: String): DownloadState {
    return downloadRequestQueue.query(Uri.parse(url))
  }

  /**
   * To check if the download was in the request queue.
   *
   * @param downloadId downalod id
   * @return true if was downloading, otherwise return false
   */
  fun isDownloading(downloadId: Int): Boolean {
    return query(downloadId) !== INVALID
  }

  /**
   * To check if the download was in the request queue.
   *
   * @param url downalod url
   * @return true if was downloading, otherwise return false
   */
  fun isDownloading(url: String): Boolean {
    return query(url) !== INVALID
  }

  /**
   * Get the download task size.
   *
   * @return the task size
   */
  val taskSize: Int
    get() = downloadRequestQueue.downloadingSize

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
  fun copyToPublicDownloadDir(filepath: String): Boolean {
    if (!filepath.startsWith(rootDownloadDir)) {
      logger.log("Only files of current app can be exported")
      return false
    }

    try {
      openOutputStream(filepath)?.use { it ->
        val fis = FileInputStream(filepath)
        copy(fis, it)
        return true
      }
    } catch (e: Exception) {
      logger.log("Failed to copy file to public download directory: " + e.message)
    }

    return false
  }

  @Suppress("DEPRECATION")
  @Throws(IOException::class) private fun openOutputStream(
    filepath: String
  ): OutputStream? {
    val filename = filepath.substring(filepath.lastIndexOf(File.separator) + 1)

    return if (VERSION.SDK_INT >= VERSION_CODES.Q) {
      val contentValues = ContentValues()
      contentValues.put(
          FileColumns.RELATIVE_PATH,
          Environment.DIRECTORY_DOWNLOADS
      )
      contentValues.put(FileColumns.DISPLAY_NAME, filename)
      val index = filename.lastIndexOf(".")
      if (index > 0) {
        val mimeType = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(filename.substring(index + 1))
        contentValues.put(FileColumns.MIME_TYPE, mimeType)
      }
      val contentResolver = context.contentResolver
      val uri = contentResolver.insert(Downloads.EXTERNAL_CONTENT_URI, contentValues)
          ?: throw IOException("Cannot get shared download directory")
      contentResolver.openOutputStream(uri)
    } else {
      val dir =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
      val outputFilepath = resolvePath(dir, filepath.substring(rootDownloadDir.length + 1))
      FileOutputStream(outputFilepath)
    }
  }

  fun newBuilder(): Builder {
    return Builder(this)
  }

  class Builder {
    var context: Context? = null
    var downloader: Downloader? = null
    var threadPoolSize: Int
    var logger: Logger

    constructor() {
      threadPoolSize = 3
      logger = Logger.EMPTY
    }

    internal constructor(downloadManager: DownloadManager) {
      context = downloadManager.context
      downloader = downloadManager.downloader
      threadPoolSize = downloadManager.threadPoolSize
      logger = downloadManager.logger
    }

    fun context(context: Context): Builder {
      this.context = context
      return this
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

    fun build(): DownloadManager {
      if (downloader == null) {
        downloader = createDefaultDownloader()
      }
      return DownloadManager(this)
    }
  }
}