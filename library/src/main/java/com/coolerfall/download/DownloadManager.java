package com.coolerfall.download;

import com.coolerfall.download.DownloadRequest.DownloadState;

/**
 * Download manager: used to manage the downloading.
 *
 * @author Vincent Cheung
 * @since Nov. 24, 2014
 */
public class DownloadManager {
	/**
	 * Custom http code invalid.
	 */
	public static final int HTTP_INVALID = 1;

	/**
	 * Custom http code error size.
	 */
	public static final int HTTP_ERROR_SIZE = 1 << 1;

	/**
	 * Custom http code error network.
	 */
	public static final int HTTP_ERROR_NETWORK = 1 << 2;

	/**
	 * Http code range not satisfiable.
	 */
	public static final int HTTP_REQUESTED_RANGE_NOT_SATISFIABLE = 416;

	/**
	 * Download request queue handles the download according to priority.
	 */
	private DownloadRequestQueue mDownloadRequestQueue;

	/**
	 * Default constructor, start the download request queue here.
	 * The default size of work pool is 3.
	 */
	public DownloadManager() {
		mDownloadRequestQueue = new DownloadRequestQueue();
		mDownloadRequestQueue.start();
	}

	/**
	 * Constructor with max thread pool size, allows maximum of 10 threads.
	 * Any number higher than 5 or less than 1, then the size will be default size.
	 * If you don't want to use default constructor to create download manager, then
	 * you can use this construtor to create a download manager with threadPoolSize.
	 *
	 * @param threadPoolSize max pool size
	 */
	public DownloadManager(int threadPoolSize) {
		mDownloadRequestQueue = new DownloadRequestQueue(threadPoolSize);
		mDownloadRequestQueue.start();
	}

	/**
	 * Add one download request into the queue.
	 *
	 * @param request download request
	 * @return download id, if the id is not set, then manager will generate one.
	 * if the request is in downloading, then -1 will be returned
	 */
	public int add(DownloadRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("DownloadRequest cannot be null");
		}

		/* if download id is not set, generate one */
		if (request.getDownloadId() == -1) {
			int downloadId = mDownloadRequestQueue.getSequenceNumber();
			request.setDownloadId(downloadId);
		}

		/* add download request into download request queue */
		return mDownloadRequestQueue.add(request) ? request.getDownloadId() : -1;
	}

	/**
	 * Query download from download request queue.
	 *
	 * @param downloadId download id
	 * @return download state
	 */
	protected DownloadState query(int downloadId) {
		return mDownloadRequestQueue.query(downloadId);
	}

	/**
	 * Query download from download request queue.
	 *
	 * @param url download url
	 * @return download state
	 */
	protected DownloadState query(String url) {
		return mDownloadRequestQueue.query(url);
	}

	/**
	 * To check if the download was in the request queue.
	 *
	 * @param downloadId downalod id
	 * @return true if was downloading, otherwise return false
	 */
	public boolean isDownloading(int downloadId) {
		return query(downloadId) != DownloadState.INVALID;
	}

	/**
	 * To check if the download was in the request queue.
	 *
	 * @param url downalod url
	 * @return true if was downloading, otherwise return false
	 */
	public boolean isDownloading(String url) {
		return query(url) != DownloadState.INVALID;
	}

	/**
	 * Get the download task size.
	 *
	 * @return the task size
	 */
	public int getTaskSize() {
		return mDownloadRequestQueue == null ? 0 : mDownloadRequestQueue.getDownloadingSize();
	}

	/**
	 * Cancel the download according to download id.
	 *
	 * @param downloadId download id
	 */
	public void cancel(int downloadId) {
		mDownloadRequestQueue.cancel(downloadId);
	}

	/**
	 * Cancel all the downloading in queue.
	 */
	public void cancelAll() {
		mDownloadRequestQueue.cancelAll();
	}

	/**
	 * Release all the resource.
	 */
	public void release() {
		if (mDownloadRequestQueue != null) {
			mDownloadRequestQueue.release();
			mDownloadRequestQueue = null;
		}
	}
}
