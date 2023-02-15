package com.coolerfall.download

import com.coolerfall.download.DownloadRequest.Builder
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowLog

/**
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
@RunWith(RobolectricTestRunner::class)
class DownloadManagerTest {
	private lateinit var mockWebServer: MockWebServer
	private lateinit var downloadManager: DownloadManager
	private lateinit var request: DownloadRequest

	@Before fun setUp() {
		ShadowLog.stream = System.out
		mockWebServer = MockWebServer()
		DownloadProvider.appContext = RuntimeEnvironment.getApplication().applicationContext
		downloadManager = DownloadManager.get()
		request = Builder()
			.url(mockWebServer.url("/").toString())
			.relativeFilepath("/shadow/download.apk")
			.build()
	}

	@After fun tearDown() {
		downloadManager.release()
		mockWebServer.shutdown()
	}

	@Test fun testDuplicate() {
		val downloadId = downloadManager.enqueue(request)
		Assert.assertEquals(1, downloadId)
		val downloadId2 = downloadManager.enqueue(request)
		Assert.assertEquals(-1, downloadId2)
	}
}