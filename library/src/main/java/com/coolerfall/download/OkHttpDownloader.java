package com.coolerfall.download;

import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.OkHttpClient;

/**
 * A downloader implemented by {@link OkHttpClient}.
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
public class OkHttpDownloader implements Downloader {
	@Override public int start(Uri uri, long breakpoint) throws IOException {
		return 0;
	}

	@Override public long contentLength() {
		return 0;
	}

	@Override public InputStream byteStream() throws IOException {
		return null;
	}

	@Override public void close() {

	}
}
