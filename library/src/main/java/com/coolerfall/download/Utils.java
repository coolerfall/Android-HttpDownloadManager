package com.coolerfall.download;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Contains some utils used in download manager.
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
class Utils {
	static final int DEFAULT_READ_TIMEOUT = 20 * 1000;
	static final int DEFAULT_WRITE_TIMEOUT = 20 * 1000;
	static final int DEFAULT_CONNECT_TIMEOUT = 15 * 1000;
	static final String HTTP = "http";
	static final String HTTPS = "https";
	static final String LOCATION = "Location";
	static final int MAX_REDIRECTION = 5;
	static final int HTTP_TEMP_REDIRECT = 307;
	static final AtomicInteger REDIRECT_TIME = new AtomicInteger(0);

	private Utils() {
	}

	/**
	 * To check whether current network is wifi.
	 *
	 * @param context context
	 * @return true if network if wifi, otherwise return false
	 */
	static boolean isWifi(Context context) {
		if (context == null) {
			return false;
		}

		ConnectivityManager manager = (ConnectivityManager) context
			.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = manager.getActiveNetworkInfo();

		return info != null && (info.getType() == ConnectivityManager.TYPE_WIFI);
	}

	/* get uuid without '-' */
	static String getUuid() {
		return UUID.randomUUID().toString().trim().replaceAll("-", "");
	}

	/* caculate md5 for string */
	static String md5(String origin) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(origin.getBytes("UTF-8"));
			BigInteger bi = new BigInteger(1, md.digest());

			return bi.toString(16);
		} catch (Exception e) {
			return getUuid();
		}
	}

	/**
	 * Get filename from url.
	 *
	 * @param url url
	 * @return filename or null if no available filename
	 */
	static String getFilenameFromUrl(String url) {
		String filename = md5(url) + ".down";

		int index = url.lastIndexOf("/");
		if (index > 0) {
			String tmpFilename = url.substring(index);
			int qmarkIndex = tmpFilename.indexOf("?");
			if (qmarkIndex > 0) {
				tmpFilename = tmpFilename.substring(0, qmarkIndex - 1);
			}

			/* if filename contains '.', then the filename has file extension */
			if (tmpFilename.contains(".")) {
				filename = tmpFilename;
			}
		}

		return filename;
	}

	/**
	 * Get real filename from http header.
	 *
	 * @param downloadUrl the url to download
	 * @return real filename
	 */
	static String getFilenameFromHeader(String downloadUrl) {
		String filename = md5(downloadUrl) + ".down";
		HttpURLConnection conn = null;
		try {
			URL url = new URL(downloadUrl);
			conn = (HttpURLConnection) url.openConnection();
			conn.setInstanceFollowRedirects(false);

			int statusCode = conn.getResponseCode();
			switch (statusCode) {
			case 301:
			case 302:
			case 303:
			case 307:
				String location = conn.getHeaderField("Location");
				/* avoid to much redirection */
				if (REDIRECT_TIME.addAndGet(1) > 5 || TextUtils.isEmpty(location)) {
					filename = getFilenameFromUrl(downloadUrl);
				} else {
					filename = getFilenameFromHeader(location);
				}
				break;

			case 200:
			default:
				/* try to get filename from content disposition */
				String contentDispos = conn.getHeaderField("Content-Disposition");
				if (!TextUtils.isEmpty(contentDispos)) {
					int index = contentDispos.indexOf("filename");
					if (index > 0) {
						filename = contentDispos.substring(
							index + 10, contentDispos.length() - 1);
					} else {
						filename = getFilenameFromUrl(downloadUrl);
					}
				} else {
					filename = getFilenameFromUrl(downloadUrl);
				}
				break;
			}
		} catch (IOException e) {
			return filename;
		} finally {
			if (conn != null) {
				conn.disconnect();
			}

			REDIRECT_TIME.set(5);
		}

		try {
			filename = URLDecoder.decode(filename, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			/* ignore */
		}

		return filename;
	}

	/**
	 * Create {@link SSLContext} for https.
	 *
	 * @return {@link SSLContext}
	 */
	static SSLContext createSSLContext() {
		try {
			SSLContext sc = SSLContext.getInstance("SSL");
			TrustManager[] tm = {
				new X509TrustManager() {
					@SuppressLint("TrustAllX509TrustManager") @Override
					public void checkClientTrusted(X509Certificate[] paramArrayOfX509Certificate,
						String paramString) throws CertificateException {
					}

					@SuppressLint("TrustAllX509TrustManager") @Override
					public void checkServerTrusted(X509Certificate[] paramArrayOfX509Certificate,
						String paramString) throws CertificateException {
					}

					@Override public X509Certificate[] getAcceptedIssuers() {
						return new X509Certificate[] {};
					}
				}
			};
			sc.init(null, tm, new SecureRandom());
			return sc;
		} catch (NoSuchAlgorithmException | KeyManagementException e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Create default {@link Downloader} for download manager.
	 *
	 * @return {@link Downloader}
	 */
	static Downloader createDefaultDownloader() {
		try {
			Class.forName("okhttp3.OkHttpClient");
			return OkHttpDownloader.create();
		} catch (ClassNotFoundException ignored) {

		}

		return URLDownloader.create();
	}
}
