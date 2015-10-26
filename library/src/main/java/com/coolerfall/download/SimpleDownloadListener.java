package com.coolerfall.download;

/**
 * Interface definition for a callback to be invoked when downloading. This
 * is a simple download listener, it's only contains two method, so if detail
 * download information is needed, then use {@link DownloadListener}
 *
 * @author Vincent Cheung
 * @since Jul. 22, 2015
 */
public interface SimpleDownloadListener {
	/**
	 * Invoked when downloading successfully.
	 *
	 * @param downloadId download id in download request queue
	 * @param filePath   file path
	 */
	void onSuccess(int downloadId, String filePath);

	/**
	 * Invoked when downloading failed.
	 *
	 * @param downloadId download id in download request queue
	 * @param statusCode status code
	 * @param errMsg     error message
	 */
	void onFailure(int downloadId, int statusCode, String errMsg);
}
