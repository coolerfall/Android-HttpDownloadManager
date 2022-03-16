package com.coolerfall.download

import android.net.Uri
import com.coolerfall.download.OkHttpDownloader.Companion.create
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
@RunWith(RobolectricTestRunner::class)
class OkHttpDownloaderTest {
  private var mockUri: Uri? = null
  private var mockWebServer: MockWebServer? = null
  private var okHttpDownloader: OkHttpDownloader? = null
  @Before @Throws(Exception::class) fun setUp() {
    mockWebServer = MockWebServer()
    mockUri = Uri.parse(
        mockWebServer!!.url("/")
            .toString()
    )
    okHttpDownloader = create()
  }

  @After @Throws(Exception::class) fun close() {
    okHttpDownloader!!.close()
  }

  @Test @Throws(Exception::class) fun testFullDownload() {
    val response = MockResponse().setHeader("Content-Length", CONTENT_LENGTH)
    mockWebServer!!.enqueue(response)
    okHttpDownloader!!.start(mockUri!!, 0)
    Assert.assertEquals(CONTENT_LENGTH.toLong(), okHttpDownloader!!.contentLength())
  }

  @Test @Throws(Exception::class) fun testBreakpointDownload() {
    val response = MockResponse().setHeader("Content-Length", CONTENT_LENGTH - 2000000)
    mockWebServer!!.enqueue(response)
    okHttpDownloader!!.start(mockUri!!, 2000000)
    Assert.assertEquals((CONTENT_LENGTH - 2000000).toLong(), okHttpDownloader!!.contentLength())
  }

  @Test @Throws(Exception::class) fun testCustomClient() {
    val client: OkHttpClient = OkHttpClient.Builder().build()
    val downloader = create(client)
    val response = MockResponse().setHeader("Content-Length", CONTENT_LENGTH)
    mockWebServer!!.enqueue(response)
    downloader.start(mockUri!!, 0)
    Assert.assertEquals(CONTENT_LENGTH.toLong(), downloader.contentLength())
    Assert.assertNotNull(downloader.byteStream())
  }

  @Test @Throws(Exception::class) fun testRedirection() {
    var response = MockResponse().setResponseCode(301)
        .addHeader("Location", mockUri.toString())
    mockWebServer!!.enqueue(response)
    response = MockResponse().setResponseCode(200)
        .setHeader("Content-Length", CONTENT_LENGTH)
    mockWebServer!!.enqueue(response)
    okHttpDownloader!!.start(mockUri!!, 0)
    Assert.assertEquals(CONTENT_LENGTH.toLong(), okHttpDownloader!!.contentLength())
  }

  companion object {
    private const val CONTENT_LENGTH = 1024 * 1024 * 5
  }
}