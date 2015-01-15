package com.cooler.download;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Contains some utils used in download manager.
 *
 * @author Vincent Cheung
 * @since  Jan. 14, 2015
 */
public class DownloadUtils {
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

		return info == null ? false : (info.getType() ==
				ConnectivityManager.TYPE_WIFI ? true : false);
	}
}
