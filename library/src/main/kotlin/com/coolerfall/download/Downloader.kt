package com.coolerfall.download

import android.net.Uri
import java.io.IOException
import java.io.InputStream

/**
 * A mechanism to download files from network. It's easy to use custom downloader
 * by implementing this interface, and then set it in [DownloadManager].
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
interface Downloader {
  /**
   * Detect filename from http header/url if existed.
   *
   * @param uri uri to detect filename
   * @return filename to save
   * @throws IOException throw [IOException] if error occurs
   */
  @Throws(IOException::class) fun detectFilename(uri: Uri): String

  /**
   * Init downloader and start to download. The downloader should handle redirect
   * status code, such as 301, 302 and so on.
   *
   * @param uri [Uri]
   * @param breakpoint breakpoint if exists
   * @return status code
   * @throws IOException throw [IOException] if error occurs
   */
  @Throws(IOException::class) fun start(
    uri: Uri,
    breakpoint: Long
  ): Int

  /**
   * Get content length for current uri.
   *
   * @return content length
   */
  fun contentLength(): Long

  /**
   * Get inputstream supported by downloader.
   *
   * @throws IOException throw [IOException] if error occurs
   */
  @Throws(IOException::class) fun byteStream(): InputStream?

  /**
   * Close downloader and stop downloader.
   */
  fun close()

  /**
   * Make a copy for this downloader.
   */
  fun copy(): Downloader
}