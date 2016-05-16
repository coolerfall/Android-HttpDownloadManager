package com.coolerfall.download;

/**
 * This will used to mark the state of download request.
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
enum DownloadState {
	/**
	 * State invalid(the request is not in queue).
	 */
	INVALID,
	/**
	 * State when the download is currently pending.
	 */
	PENDING,
	/**
	 * State when the download is currently running.
	 */
	RUNNING,
	/**
	 * State when the download is successful.
	 */
	SUCCESSFUL,
	/**
	 * State when the download is failed.
	 */
	FAILURE,
}
