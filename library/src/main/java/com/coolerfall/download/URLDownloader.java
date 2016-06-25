package com.coolerfall.download;

import android.annotation.SuppressLint;
import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;

import static java.net.HttpURLConnection.HTTP_MOVED_PERM;
import static java.net.HttpURLConnection.HTTP_MOVED_TEMP;
import static java.net.HttpURLConnection.HTTP_SEE_OTHER;
import static com.coolerfall.download.Utils.HTTP;
import static com.coolerfall.download.Utils.HTTPS;

/**
 * A default downloader implemented by {@link URLConnection}.
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
public final class URLDownloader implements Downloader {
	private static final String ACCPET_ENCODING = "Accept-Encoding";
	private static final String TRANSFER_ENCODING = "Transfer-Encoding";
	private static final String CONTENT_LENGTH = "Content-Length";

	private int redirectionCount = 0;
	private HttpURLConnection httpURLConnection;

	private URLDownloader() {
	}

	/**
	 * Create an instance using {@link URLConnection}.
	 *
	 * @return {@link URLDownloader}
	 */
	public static URLDownloader create() {
		return new URLDownloader();
	}

	@Override public int start(Uri uri, long breakpoint) throws IOException {
		String scheme = uri.getScheme();
		if (!HTTP.equals(scheme) && !HTTPS.equals(scheme)) {
			throw new DownloadException(0, "url should start with http or https");
		}

		URL url = new URL(uri.toString());
		if (HTTPS.equals(scheme)) {
			HttpsURLConnection httpsURLConnection = (HttpsURLConnection) url.openConnection();
			SSLContext sslContext = Utils.createSSLContext();
			if (sslContext != null) {
				SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
				httpsURLConnection.setSSLSocketFactory(sslSocketFactory);
				httpsURLConnection.setHostnameVerifier(new HostnameVerifier() {
					@SuppressLint("BadHostnameVerifier") @Override
					public boolean verify(String hostname, SSLSession session) {
						return true;
					}
				});
			}
			httpURLConnection = httpsURLConnection;
		} else {
			httpURLConnection = (HttpURLConnection) url.openConnection();
		}

		httpURLConnection.setInstanceFollowRedirects(false);
		httpURLConnection.setUseCaches(false);
		httpURLConnection.setRequestProperty(ACCPET_ENCODING, "identity");
		httpURLConnection.setConnectTimeout(Utils.DEFAULT_CONNECT_TIMEOUT);
		httpURLConnection.setReadTimeout(Utils.DEFAULT_READ_TIMEOUT);
		if (breakpoint > 0) {
			httpURLConnection.setRequestProperty("Range", "bytes=" + breakpoint + "-");
		}

		int statusCode = httpURLConnection.getResponseCode();
		switch (statusCode) {
		case HTTP_MOVED_PERM:
		case HTTP_MOVED_TEMP:
		case HTTP_SEE_OTHER:
		case Utils.HTTP_TEMP_REDIRECT:
			if (redirectionCount++ < Utils.MAX_REDIRECTION) {
			    /* take redirect url and call executeDownload recursively */
				String redirectUrl = httpURLConnection.getHeaderField(Utils.LOCATION);
				httpURLConnection.disconnect();
				return start(Uri.parse(redirectUrl), breakpoint);
			} else {
				throw new DownloadException(statusCode, httpURLConnection.getResponseMessage());
			}

		default:
			return statusCode;
		}
	}

	@Override public long contentLength() {
		return getContentLength(httpURLConnection);
	}

	@Override public InputStream byteStream() throws IOException {
		return httpURLConnection.getInputStream();
	}

	@Override public void close() {
		if (httpURLConnection != null) {
			httpURLConnection.disconnect();
		}
	}

	@Override public Downloader copy() {
		return create();
	}

	/* read response content length from server */
	int getContentLength(HttpURLConnection conn) {
		String transferEncoding = conn.getHeaderField(TRANSFER_ENCODING);
		if (transferEncoding == null || transferEncoding.equalsIgnoreCase("chunked")) {
			return conn.getHeaderFieldInt(CONTENT_LENGTH, -1);
		} else {
			return -1;
		}
	}
}
