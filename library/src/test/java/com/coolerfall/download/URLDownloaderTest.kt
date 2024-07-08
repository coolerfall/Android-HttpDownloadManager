package com.coolerfall.download

import android.net.Uri
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
class URLDownloaderTest {
	private lateinit var mockUri: Uri
	private lateinit var mockWebServer: MockWebServer
	private lateinit var urlDownloader: URLDownloader

	companion object {
		private const val CONTENT_LENGTH = 1024 * 1024 * 5
	}

	@Before fun setUp() {
		mockWebServer = MockWebServer()
		mockUri = Uri.parse(mockWebServer.url("/").toString())
		urlDownloader = URLDownloader.create()
	}

	@After fun close() {
		urlDownloader.close()
	}

	@Test fun testFullDownload() {
		val response = MockResponse().setResponseCode(200)
			.setHeader("Content-Length", CONTENT_LENGTH)
		mockWebServer.enqueue(response)
		urlDownloader.start(mockUri, 0)
		Assert.assertEquals(CONTENT_LENGTH.toLong(), urlDownloader.contentLength())
		Assert.assertNotNull(urlDownloader.byteStream())
	}

	@Test fun testBreakpointDownload() {
		val response = MockResponse().setResponseCode(200)
			.setHeader("Content-Length", CONTENT_LENGTH - 10000)
		mockWebServer.enqueue(response)
		urlDownloader.start(mockUri, 10000)
		Assert.assertEquals((CONTENT_LENGTH - 10000).toLong(), urlDownloader.contentLength())
	}

	@Test fun testRedirection() {
		var response = MockResponse().setResponseCode(307)
			.addHeader("Location", mockUri.toString())
		mockWebServer.enqueue(response)
		response = MockResponse().setResponseCode(200)
			.setHeader("Content-Length", CONTENT_LENGTH)
		mockWebServer.enqueue(response)
		urlDownloader.start(mockUri, 0)
		Assert.assertEquals(CONTENT_LENGTH.toLong(), urlDownloader.contentLength())
	}
}