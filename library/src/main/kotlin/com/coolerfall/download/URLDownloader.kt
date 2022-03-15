package com.coolerfall.download

import android.net.Uri
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.HttpsURLConnection

/**
 * A default downloader implemented by [URLConnection].
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
class URLDownloader private constructor() : Downloader {

  private var httpURLConnection: HttpURLConnection? = null
  private val redirectionCount = AtomicInteger()

  companion object {
    private const val ACCPET_ENCODING = "Accept-Encoding"
    private const val TRANSFER_ENCODING = "Transfer-Encoding"
    private const val CONTENT_LENGTH = "Content-Length"

    /**
     * Create an instance using [URLConnection].
     *
     * @return [URLDownloader]
     */
    @JvmStatic fun create(): URLDownloader {
      return URLDownloader()
    }
  }

  @Throws(IOException::class) override fun detectFilename(uri: Uri): String {
    redirectionCount.set(Helper.MAX_REDIRECTION)
    val httpURLConnection = innerRequest(uri, 0)
    val url = httpURLConnection.url.toString()
    val contentDispisition = httpURLConnection.getHeaderField(Helper.CONTENT_DISPOSITION)
    httpURLConnection.disconnect()
    return Helper.getFilenameFromHeader(url, contentDispisition)
  }

  @Throws(IOException::class) override fun start(
    uri: Uri,
    breakpoint: Long
  ): Int {
    redirectionCount.set(Helper.MAX_REDIRECTION)
    httpURLConnection = innerRequest(uri, breakpoint)
    return httpURLConnection!!.responseCode
  }

  override fun contentLength(): Long {
    return getContentLength(httpURLConnection).toLong()
  }

  @Throws(IOException::class) override fun byteStream(): InputStream? {
    return httpURLConnection!!.inputStream
  }

  override fun close() {
    if (httpURLConnection != null) {
      httpURLConnection!!.disconnect()
    }
  }

  override fun copy(): Downloader {
    return create()
  }

  @Throws(IOException::class) fun innerRequest(
    uri: Uri,
    breakpoint: Long
  ): HttpURLConnection {
    val httpURLConnection: HttpURLConnection
    val url = URL(uri.toString())
    httpURLConnection = if (Helper.HTTPS == uri.scheme) {
      val httpsURLConnection = url.openConnection() as HttpsURLConnection
      httpsURLConnection
    } else {
      url.openConnection() as HttpURLConnection
    }
    httpURLConnection.instanceFollowRedirects = true
    httpURLConnection.useCaches = false
    httpURLConnection.setRequestProperty(ACCPET_ENCODING, "identity")
    httpURLConnection.connectTimeout = Helper.DEFAULT_CONNECT_TIMEOUT
    httpURLConnection.readTimeout = Helper.DEFAULT_READ_TIMEOUT
    if (breakpoint > 0) {
      httpURLConnection.setRequestProperty("Range", "bytes=$breakpoint-")
    }
    return when (val statusCode = httpURLConnection.responseCode) {
      HttpURLConnection.HTTP_MOVED_PERM,
      HttpURLConnection.HTTP_MOVED_TEMP,
      HttpURLConnection.HTTP_SEE_OTHER,
      Helper.HTTP_TEMP_REDIRECT ->
        if (redirectionCount.decrementAndGet() >= 0) {
          /* take redirect url and call start recursively */
          val redirectUrl = httpURLConnection.getHeaderField(Helper.LOCATION)
          httpURLConnection.disconnect()
          if (redirectUrl == null) {
            throw DownloadException(statusCode, "redirects got no `Location` header")
          }
          innerRequest(Uri.parse(redirectUrl), breakpoint)
        } else {
          throw DownloadException(statusCode, "redirects too many times")
        }
      else -> httpURLConnection
    }
  }

  /* read response content length from server */
  private fun getContentLength(conn: HttpURLConnection?): Int {
    val transferEncoding = conn!!.getHeaderField(TRANSFER_ENCODING)
    return if (transferEncoding == null || transferEncoding.equals("chunked", ignoreCase = true)) {
      conn.getHeaderFieldInt(CONTENT_LENGTH, -1)
    } else {
      -1
    }
  }
}