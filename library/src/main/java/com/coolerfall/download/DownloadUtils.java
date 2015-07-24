package com.coolerfall.download;

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
import java.security.MessageDigest;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Contains some utils used in download manager.
 *
 * @author Vincent Cheung
 * @since  Jan. 14, 2015
 */
public class DownloadUtils {
	private static final AtomicInteger REDIRECT_TIME = new AtomicInteger(0);

	/**
	 * To check whether current network is wifi.
	 *
	 * @param  context context
	 * @return         true if network if wifi, otherwise return false
	 */
	protected static boolean isWifi(Context context) {
		if (context == null) {
			return false;
		}

		ConnectivityManager manager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = manager.getActiveNetworkInfo();

		return info != null && (info.getType() == ConnectivityManager.TYPE_WIFI);
	}

	/* get uuid without '-' */
	private static String getUuid() {
		return UUID.randomUUID().toString().trim().replaceAll("-", "");
	}

	/* caculate md5 for string */
	private static String md5(String origin) {
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
	 * @param  url url
	 * @return     filename or null if no available filename
	 */
	protected static String getFilenameFromUrl(String url) {
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
	 * @param  downloadUrl the url to download
	 * @return             real filename
	 */
	protected static String getFilenameFromHeader(String downloadUrl) {
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
}
