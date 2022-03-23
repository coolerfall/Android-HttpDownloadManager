package com.coolerfall.download

import com.coolerfall.download.DownloadManager.Builder
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

/**
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], manifest = Config.NONE)
class DownloadManagerTest {
  private var mockWebServer: MockWebServer? = null
  private var downloadManager: DownloadManager? = null
  private var request: DownloadRequest? = null
  @Before @Throws(Exception::class) fun setUp() {
    ShadowLog.stream = System.out
    mockWebServer = MockWebServer()
    downloadManager = Builder().context(
        RuntimeEnvironment.getApplication().applicationContext
    )
        .build()
    request = DownloadRequest.Builder()
        .url(
            mockWebServer!!.url("/")
                .toString()
        )
        .relativeFilepath("/shadow/download.apk")
        .build()
  }

  @After @Throws(Exception::class) fun tearDown() {
    downloadManager!!.release()
    mockWebServer!!.shutdown()
  }

  @Test @Throws(Exception::class) fun testDownload() {
    val downloadId = downloadManager!!.add(request!!)
    Assert.assertEquals(1, downloadId.toLong())
  }

  @Test @Throws(Exception::class) fun testDuplicate() {
    val downloadId = downloadManager!!.add(request!!)
    Assert.assertEquals(1, downloadId.toLong())
    val downloadId2 = downloadManager!!.add(request!!)
    Assert.assertEquals(-1, downloadId2.toLong())
  }
}