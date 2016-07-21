package com.coolerfall.download;

import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;

import static com.coolerfall.download.Utils.CONTENT_DISPOSITION;
import static com.coolerfall.download.Utils.DEFAULT_CONNECT_TIMEOUT;
import static com.coolerfall.download.Utils.DEFAULT_READ_TIMEOUT;
import static com.coolerfall.download.Utils.DEFAULT_WRITE_TIMEOUT;
import static com.coolerfall.download.Utils.HTTP;
import static com.coolerfall.download.Utils.HTTPS;
import static com.coolerfall.download.Utils.HTTP_TEMP_REDIRECT;
import static com.coolerfall.download.Utils.LOCATION;
import static com.coolerfall.download.Utils.MAX_REDIRECTION;
import static com.coolerfall.download.Utils.getFilenameFromHeader;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * A downloader implemented by {@link OkHttpClient}.
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
public final class OkHttpDownloader implements Downloader {
	private final OkHttpClient client;
	private Response response;
	private int redirectionCount = 0;

	private static OkHttpClient defaultOkHttpClient() {
		return new OkHttpClient.Builder().connectTimeout(DEFAULT_CONNECT_TIMEOUT, MILLISECONDS)
			.readTimeout(DEFAULT_READ_TIMEOUT, MILLISECONDS)
			.writeTimeout(DEFAULT_WRITE_TIMEOUT, MILLISECONDS)
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

	@Override public String detectFilename(Uri uri) throws IOException {
		redirectionCount = 0;
		Response response = innerRequest(client, uri, 0);
		String url = response.request().url().toString();
		String contentDisposition = response.header(CONTENT_DISPOSITION);
		response.close();
		return getFilenameFromHeader(url, contentDisposition);
	}

	@Override public int start(Uri uri, long breakpoint) throws IOException {
		redirectionCount = 0;
		response = innerRequest(client, uri, breakpoint);
		return response.code();
	}

	@Override public long contentLength() {
		return getContentLength(response.body());
	}

	@Override public InputStream byteStream() {
		return response == null ? null : response.body().byteStream();
	}

	@Override public void close() {
		if (response != null) {
			response.close();
		}
	}

	@Override public Downloader copy() {
		return create(client.newBuilder().build());
	}

	Response innerRequest(OkHttpClient client, Uri uri, long breakpoint) throws IOException {
		String scheme = uri.getScheme();
		if (!HTTP.equals(scheme) && !HTTPS.equals(scheme)) {
			throw new DownloadException(0, "url should start with http or https");
		}

		Request.Builder builder = new Request.Builder().url(uri.toString());
		if (breakpoint > 0) {
			builder.header("Accept-Encoding", "identity")
				.header("Range", "bytes=" + breakpoint + "-")
				.build();
		}
		Response response = client.newCall(builder.build()).execute();
		int statusCode = response.code();
		switch (statusCode) {
		case 301:
		case 302:
		case 303:
		case HTTP_TEMP_REDIRECT:
			response.close();
			if (redirectionCount++ < MAX_REDIRECTION) {
			    /* take redirect url and call start recursively */
				String redirectUrl = response.header(LOCATION);
				return innerRequest(client, Uri.parse(redirectUrl), breakpoint);
			} else {
				throw new DownloadException(statusCode, "redirects too many times");
			}
		}

		return response;
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
