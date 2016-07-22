package com.coolerfall.download;

import android.net.Uri;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
@RunWith(RobolectricGradleTestRunner.class) @Config(constants = BuildConfig.class, sdk = 21)
public class URLDownloaderTest {
	private static final int CONTENT_LENGTH = 1024 * 1024 * 5;
	private Uri mockUri;
	private MockWebServer mockWebServer;
	private URLDownloader urlDownloader;

	@Before public void setUp() throws Exception {
		mockWebServer = new MockWebServer();
		mockUri = Uri.parse(mockWebServer.url("/").toString());
		urlDownloader = URLDownloader.create();
	}

	@After public void close() throws Exception {
		urlDownloader.close();
	}

	@Test public void testFullDownload() throws Exception {
		MockResponse response =
			new MockResponse().setResponseCode(200).setHeader("Content-Length", CONTENT_LENGTH);
		mockWebServer.enqueue(response);
		urlDownloader.start(mockUri, 0);
		assertEquals(CONTENT_LENGTH, urlDownloader.contentLength());
		assertNotNull(urlDownloader.byteStream());
	}

	@Test public void testBreakpointDownload() throws Exception {
		MockResponse response =
			new MockResponse().setResponseCode(200)
				.setHeader("Content-Length", CONTENT_LENGTH - 10000);
		mockWebServer.enqueue(response);
		urlDownloader.start(mockUri, 10000);
		assertEquals(CONTENT_LENGTH - 10000, urlDownloader.contentLength());
	}

	@Test public void testRedirection() throws Exception {
		MockResponse response =
			new MockResponse().setResponseCode(307).addHeader("Location", mockUri.toString());
		mockWebServer.enqueue(response);
		response =
			new MockResponse().setResponseCode(200).setHeader("Content-Length", CONTENT_LENGTH);
		mockWebServer.enqueue(response);
		urlDownloader.start(mockUri, 0);
		assertEquals(CONTENT_LENGTH, urlDownloader.contentLength());
	}

	@Test public void testBadUrl() {
		try {
			urlDownloader.start(Uri.parse("www.baidu.com"), 0);
		} catch (Exception e) {
			if (e instanceof IllegalArgumentException) {
				assertEquals("unexpected url: www.baidu.com", e.getMessage());
			}
		}
	}
}
