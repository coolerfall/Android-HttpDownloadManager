package com.coolerfall.download

import android.net.Uri
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
	private lateinit var mockUri: Uri
	private lateinit var mockWebServer: MockWebServer
	private lateinit var okHttpDownloader: OkHttpDownloader

	companion object {
		private const val CONTENT_LENGTH = 1024 * 1024 * 5
	}

	@Before fun setUp() {
		mockWebServer = MockWebServer()
		mockUri = Uri.parse(mockWebServer.url("/").toString())
		okHttpDownloader = OkHttpDownloader.create()
	}

	@After fun close() {
		okHttpDownloader.close()
	}

	@Test fun testFullDownload() {
		val response = MockResponse().setHeader("Content-Length", CONTENT_LENGTH)
		mockWebServer.enqueue(response)
		okHttpDownloader.start(mockUri, 0)
		Assert.assertEquals(CONTENT_LENGTH.toLong(), okHttpDownloader.contentLength())
	}

	@Test fun testBreakpointDownload() {
		val response = MockResponse().setHeader("Content-Length", CONTENT_LENGTH - 2000000)
		mockWebServer.enqueue(response)
		okHttpDownloader.start(mockUri, 2000000)
		Assert.assertEquals((CONTENT_LENGTH - 2000000).toLong(), okHttpDownloader.contentLength())
	}

	@Test fun testCustomClient() {
		val client: OkHttpClient = OkHttpClient.Builder().build()
		val downloader = OkHttpDownloader.create(client)
		val response = MockResponse().setHeader("Content-Length", CONTENT_LENGTH)
		mockWebServer.enqueue(response)
		downloader.start(mockUri, 0)
		Assert.assertEquals(CONTENT_LENGTH.toLong(), downloader.contentLength())
		Assert.assertNotNull(downloader.byteStream())
	}

	@Test fun testRedirection() {
		var response = MockResponse().setResponseCode(301)
			.addHeader("Location", mockUri.toString())
		mockWebServer.enqueue(response)
		response = MockResponse().setResponseCode(200)
			.setHeader("Content-Length", CONTENT_LENGTH)
		mockWebServer.enqueue(response)
		okHttpDownloader.start(mockUri, 0)
		Assert.assertEquals(CONTENT_LENGTH.toLong(), okHttpDownloader.contentLength())
	}
}