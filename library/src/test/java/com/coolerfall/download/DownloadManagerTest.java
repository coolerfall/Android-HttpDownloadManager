package com.coolerfall.download;

import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import static org.junit.Assert.assertEquals;

/**
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
@RunWith(RobolectricTestRunner.class) @Config(sdk = 29, manifest = Config.NONE)
public class DownloadManagerTest {
  private MockWebServer mockWebServer;
  private DownloadManager downloadManager;
  private DownloadRequest request;

  @Before public void setUp() throws Exception {
    ShadowLog.stream = System.out;
    mockWebServer = new MockWebServer();
    downloadManager = new DownloadManager.Builder().context(
        RuntimeEnvironment.getApplication().getApplicationContext()).build();
    request = new DownloadRequest.Builder().url(mockWebServer.url("/").toString())
        .relativeFilePath("/shadow/download.apk")
        .build();
  }

  @After public void tearDown() throws Exception {
    downloadManager.release();
    mockWebServer.shutdown();
  }

  @Test public void testDownload() throws Exception {
    int downloadId = downloadManager.add(request);
    assertEquals(1, downloadId);
  }

  @Test public void testDuplicate() throws Exception {
    int downloadId = downloadManager.add(request);
    assertEquals(1, downloadId);
    int downloadId2 = downloadManager.add(request);
    assertEquals(-1, downloadId2);
  }
}
