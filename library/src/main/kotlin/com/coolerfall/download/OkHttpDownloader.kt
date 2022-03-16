package com.coolerfall.download

import okhttp3.OkHttpClient
import okhttp3.Response
import java.util.concurrent.atomic.AtomicInteger
import kotlin.Throws
import java.io.IOException
import android.net.Uri
import okhttp3.Request
import java.io.InputStream
import java.util.concurrent.TimeUnit.MILLISECONDS

/**
 * A downloader implemented by [OkHttpClient].
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
class OkHttpDownloader private constructor(client: OkHttpClient?) : Downloader {
  private val client: OkHttpClient = client ?: defaultOkHttpClient()
  private var response: Response? = null
  private val redirectionCount = AtomicInteger()

  companion object {

    private fun defaultOkHttpClient(): OkHttpClient {
      return OkHttpClient.Builder()
          .connectTimeout(Helper.DEFAULT_CONNECT_TIMEOUT.toLong(), MILLISECONDS)
          .readTimeout(Helper.DEFAULT_READ_TIMEOUT.toLong(), MILLISECONDS)
          .writeTimeout(Helper.DEFAULT_WRITE_TIMEOUT.toLong(), MILLISECONDS)
          .build()
    }

    /**
     * Create an instance using a default [OkHttpClient].
     *
     * @return [OkHttpDownloader]
     */
    @JvmStatic fun create(): OkHttpDownloader {
      return OkHttpDownloader(null)
    }

    /**
     * Create an instance using a `client`.
     *
     * @return [OkHttpDownloader]
     */
    @JvmStatic fun create(client: OkHttpClient?): OkHttpDownloader {
      return OkHttpDownloader(client)
    }
  }

  @Throws(IOException::class) override fun detectFilename(uri: Uri): String {
    redirectionCount.set(Helper.MAX_REDIRECTION)
    val response = innerRequest(client, uri, 0)
    val url = response.request.url.toString()
    val contentDisposition = response.header(Helper.CONTENT_DISPOSITION)
    response.close()
    return Helper.getFilenameFromHeader(url, contentDisposition)
  }

  @Throws(IOException::class) override fun start(
    uri: Uri,
    breakpoint: Long
  ): Int {
    redirectionCount.set(Helper.MAX_REDIRECTION)
    response = innerRequest(client, uri, breakpoint)
    return response!!.code
  }

  override fun contentLength(): Long {
    return response?.body?.contentLength() ?: -1
  }

  override fun byteStream(): InputStream? {
    return response?.body?.byteStream()
  }

  override fun close() {
    response?.close()
  }

  override fun copy(): Downloader {
    return create(client)
  }

  @Throws(IOException::class) fun innerRequest(
    client: OkHttpClient,
    uri: Uri,
    breakpoint: Long
  ): Response {
    val builder: Request.Builder = Request.Builder()
        .url(uri.toString())
    if (breakpoint > 0) {
      builder.header("Accept-Encoding", "identity")
          .header("Range", "bytes=$breakpoint-")
          .build()
    }
    val response = client.newCall(builder.build())
        .execute()
    when (val statusCode = response.code) {
      301, 302, 303, Helper.HTTP_TEMP_REDIRECT -> {
        response.close()
        return if (redirectionCount.decrementAndGet() >= 0) {
          /* take redirect url and call start recursively */
          val redirectUrl = response.header(Helper.LOCATION)
              ?: throw DownloadException(statusCode, "redirects got no `Location` header")
          innerRequest(client, Uri.parse(redirectUrl), breakpoint)
        } else {
          throw DownloadException(statusCode, "redirects too many times")
        }
      }
    }
    return response
  }
}