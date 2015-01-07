package com.cooler.download;

import com.cooler.download.DownloadRequest.DownloadState;

/**
 * Download manager: used to manage the downloading.
 * 
 * @author Vincent Cheung
 * @since  Nov. 24, 2014
 */
public class DownloadManager {
	/** custom http code invalid */
	public static final int HTTP_INVALID = 1 << 0;
	
	/** custom http code error size */
	public static final int HTTP_ERROR_SIZE = 1 << 1;
	
	/** range not satisfiable */
	public static final int HTTP_REQUESTED_RANGE_NOT_SATISFIABLE = 416;
	
	/** download request queue handles the download according to priority */
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
	 * Constructor with max thread pool size, allows maximum of 5 threads.
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
	 */
	public void add(DownloadRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("DownloadRequest cannot be null");
		}
		
		mDownloadRequestQueue.add(request);
	}
	
	/**
	 * Query download from download request queue.
	 * 
	 * @param  downloadId download id
	 * @return            download state
	 */
	protected DownloadState query(int downloadId) {
		return mDownloadRequestQueue.query(downloadId);
	}
	
	/**
	 * To check if the download was in the request queue.
	 * 
	 * @param  downloadId downalod id
	 * @return            true if was downloading, otherwise return false
	 */
	public boolean isDownloading(int downloadId) {
		if (query(downloadId) == DownloadState.INVALID) {
			return false;
		}
		
		return true;
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
