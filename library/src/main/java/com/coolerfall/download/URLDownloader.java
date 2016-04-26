package com.coolerfall.download;

import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

/**
 * A default downloader impelemted by {@link URLConnection}.
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
public class URLDownloader implements Downloader {
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
