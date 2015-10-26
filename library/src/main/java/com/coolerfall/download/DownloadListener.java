package com.coolerfall.download;

/**
 * Interface definition for a callback to be invoked when downloading.
 * This download listener contains detail download information. If simple listener
 * is needed, then use {@link SimpleDownloadListener}
 *
 * @author Vincent Cheung
 * @since Nov. 24, 2014
 */
public interface DownloadListener {
	/**
	 * Invoked when downloading is started.
	 *
	 * @param downloadId download id in download request queue
	 * @param totalBytes total bytes of the file
	 */
	void onStart(int downloadId, long totalBytes);

	/**
	 * Invoked when download retrying.
	 *
	 * @param downloadId download id in download request queue
	 */
	void onRetry(int downloadId);

	/**
	 * Invoked when downloading is in progress.
	 *
	 * @param downloadId   download id in download request queue
	 * @param bytesWritten the bytes has written to local disk
	 * @param totalBytes   total bytes of the file
	 */
	void onProgress(int downloadId, long bytesWritten, long totalBytes);

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
