package com.coolerfall.download;

import android.os.Environment;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowEnvironment;
import org.robolectric.shadows.ShadowLog;

import java.io.File;

import okhttp3.mockwebserver.MockWebServer;

import static org.junit.Assert.assertEquals;

/**
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
@RunWith(RobolectricTestRunner.class) @Config(constants = BuildConfig.class, sdk = 21)
public class DownloadManagerTest {
	private MockWebServer mockWebServer;
	private DownloadManager downloadManager;
	private DownloadRequest request;

	@Before public void setUp() throws Exception {
		ShadowLog.stream = System.out;
		mockWebServer = new MockWebServer();
		downloadManager = new DownloadManager.Builder().context(
			ShadowApplication.getInstance().getApplicationContext()).build();
		String filePath =
			ShadowEnvironment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
				+ File.separator
				+ "download.apk";

		request =
			new DownloadRequest.Builder().url(mockWebServer.url("/").toString())
				.destinationFilePath(filePath)
				.build();
	}

	@After
	public void tearDown() throws Exception {
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
