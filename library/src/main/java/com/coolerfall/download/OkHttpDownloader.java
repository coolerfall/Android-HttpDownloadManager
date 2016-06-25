package com.coolerfall.download;

import android.annotation.SuppressLint;
import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;

import static com.coolerfall.download.Utils.HTTP;
import static com.coolerfall.download.Utils.HTTPS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * A downloader implemented by {@link OkHttpClient}.
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
public final class OkHttpDownloader implements Downloader {
	private OkHttpClient client;
	private ResponseBody body;
	private int redirectionCount = 0;

	private static OkHttpClient defaultOkHttpClient() {
		return new OkHttpClient.Builder().connectTimeout(Utils.DEFAULT_CONNECT_TIMEOUT,
			MILLISECONDS)
			.readTimeout(Utils.DEFAULT_READ_TIMEOUT, MILLISECONDS)
			.writeTimeout(Utils.DEFAULT_WRITE_TIMEOUT, MILLISECONDS)
			.build();
	}

	/**
	 * Create an instance using a default {@link OkHttpClient}.
	 *
	 * @return {@link OkHttpDownloader}
	 */
	public static OkHttpDownloader create() {
		return new OkHttpDownloader(null);
	}

	/**
	 * Create an instance using a {@code client}.
	 *
	 * @return {@link OkHttpDownloader}
	 */
	public static OkHttpDownloader create(OkHttpClient client) {
		return new OkHttpDownloader(client);
	}

	private OkHttpDownloader(OkHttpClient client) {
		this.client = client == null ? defaultOkHttpClient() : client;
	}

	@Override public int start(Uri uri, long breakpoint) throws IOException {
		String scheme = uri.getScheme();
		if (!HTTP.equals(scheme) && !HTTPS.equals(scheme)) {
			throw new DownloadException(0, "url should start with http or https");
		}

		OkHttpClient.Builder okHttpbuilder = client.newBuilder();
		SSLContext sslContext = Utils.createSSLContext();
		if (sslContext != null) {
			SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
			okHttpbuilder.sslSocketFactory(sslSocketFactory);
			okHttpbuilder.hostnameVerifier(new HostnameVerifier() {
				@SuppressLint("BadHostnameVerifier") @Override
				public boolean verify(String hostname, SSLSession session) {
					return true;
				}
			});
			client = okHttpbuilder.build();
		}

		Request.Builder builder = new Request.Builder().url(uri.toString());
		if (breakpoint > 0) {
			builder.header("Accept-Encoding", "identity")
				.header("Range", "bytes=" + breakpoint + "-")
				.build();
		}
		Response response = client.newCall(builder.build()).execute();
		int statusCode = response.code();
		body = response.body();
		switch (statusCode) {
		case 200:
		case 206:
			return statusCode;

		case 301:
		case 302:
		case 303:
		case Utils.HTTP_TEMP_REDIRECT:
			body.close();
			if (redirectionCount++ < Utils.MAX_REDIRECTION) {
			    /* take redirect url and call executeDownload recursively */
				String redirectUrl = response.header(Utils.LOCATION);
				return start(Uri.parse(redirectUrl), breakpoint);
			} else {
				throw new DownloadException(statusCode, response.message());
			}
		}

		return statusCode;
	}

	@Override public long contentLength() {
		return getContentLength(body);
	}

	@Override public InputStream byteStream() {
		return body.byteStream();
	}

	@Override public void close() {
		if (body != null) {
			body.close();
		}
	}

	@Override public Downloader copy() {
		return create(client);
	}

	/* read response content length from server */
	long getContentLength(ResponseBody body) {
		if (body == null) {
			return -1;
		}

		long contentLength = body.contentLength();
		if (contentLength <= 0) {
			BufferedSource source = body.source();
			try {
				source.request(Long.MAX_VALUE);
				Buffer buffer = source.buffer();
				contentLength = buffer.size();
			} catch (IOException e) {
				return -1;
			}
		}

		return contentLength;
	}
}
