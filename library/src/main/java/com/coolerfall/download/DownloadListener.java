package com.coolerfall.download;

/**
 * Interface definition for a callback to be invoked when downloading.
 * 
 * @author Vincent Cheung
 * @since  Nov. 24, 2014
 */
public interface DownloadListener {
	/**
	 * Invoked when downloading is started.
	 */
	void onStart(int downloadId, long totalBytes);
	/**
	 * Invoked when download retrying.
	 */
	void onRetry(int downloadId);
	/**
	 * Invoked when downloading is in progress.
	 */
	void onProgress(int downloadId, long bytesWritten, long totalBytes);
	/**
	 * Invoked when downloading successfully.
	 */
	void onSuccess(int downloadId, String filePath);
	/**
	 * Invoked when downloading failed.
	 */
	void onFailure(int downloadId, int statusCode, String errMsg);
}
