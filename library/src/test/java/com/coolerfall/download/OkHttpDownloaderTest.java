package com.coolerfall.download;

import android.net.Uri;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
@RunWith(RobolectricGradleTestRunner.class) @Config(constants = BuildConfig.class, sdk = 23)
public class OkHttpDownloaderTest {
	private static final int CONTENT_LENGTH = 1024 * 1024 * 5;
	private Uri mockUri;
	private MockWebServer mockWebServer;
	private OkHttpDownloader okHttpDownloader;

	@Before public void setUp() throws Exception {
		mockWebServer = new MockWebServer();
		mockUri = Uri.parse(mockWebServer.url("/").toString());
		okHttpDownloader = OkHttpDownloader.create();
	}

	@After public void close() throws Exception {
		okHttpDownloader.close();
	}

	@Test public void testFullDownload() throws Exception {
		MockResponse response = new MockResponse().setHeader("Content-Length", CONTENT_LENGTH);
		mockWebServer.enqueue(response);
		okHttpDownloader.start(mockUri, 0);
		assertEquals(CONTENT_LENGTH, okHttpDownloader.contentLength());
	}

	@Test public void testBreakpointDownload() throws Exception {
		MockResponse response =
			new MockResponse().setHeader("Content-Length", CONTENT_LENGTH - 2000000);
		mockWebServer.enqueue(response);
		okHttpDownloader.start(mockUri, 2000000);
		assertEquals(CONTENT_LENGTH - 2000000, okHttpDownloader.contentLength());
	}

	@Test public void testCustomClient() throws Exception {
		OkHttpClient client = new OkHttpClient.Builder().build();
		OkHttpDownloader downloader = OkHttpDownloader.create(client);
		MockResponse response = new MockResponse().setHeader("Content-Length", CONTENT_LENGTH);
		mockWebServer.enqueue(response);
		downloader.start(mockUri, 0);
		assertEquals(CONTENT_LENGTH, downloader.contentLength());
		assertNotNull(downloader.byteStream());
	}

	@Test public void testRedirection() throws Exception {
		MockResponse response =
			new MockResponse().setResponseCode(301).addHeader("Location", mockUri.toString());
		mockWebServer.enqueue(response);
		response =
			new MockResponse().setResponseCode(200).setHeader("Content-Length", CONTENT_LENGTH);
		mockWebServer.enqueue(response);
		okHttpDownloader.start(mockUri, 0);
		assertEquals(CONTENT_LENGTH, okHttpDownloader.contentLength());
	}

	@Test public void testBadUrl() {
		try {
			okHttpDownloader.start(Uri.parse("www.baidu.com"), 0);
		} catch (Exception e) {
			if (e instanceof IllegalArgumentException) {
				assertEquals("unexpected url: www.baidu.com", e.getMessage());
			}
		}
	}
}
