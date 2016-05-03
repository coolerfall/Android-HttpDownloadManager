package com.coolerfall.download;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Download request queue, this is designed according to RequestQueue in Andoird-Volley.
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
final class DownloadRequestQueue {
	private static final String TAG = DownloadRequestQueue.class.getSimpleName();

	/**
	 * The capacity of download request queue.
	 */
	private static final int CAPACITY = 20;

	/**
	 * The default number of download dispatcher threads.
	 */
	private static final int DEFAULT_DOWNLOAD_THREAD_POOL_SIZE = 3;

	/**
	 * The set of all requests currently being processed by this DownloadQueue. A Request
	 * will be in this set if it is waiting in any queue or currently being processed by
	 * any dispatcher.
	 */
	private final Set<DownloadRequest> mCurrentRequests = new HashSet<>();

	/**
	 * The queue of download request.
	 */
	private PriorityBlockingQueue<DownloadRequest> mDownloadQueue =
		new PriorityBlockingQueue<>(CAPACITY);

	/**
	 * The download dispatchers.
	 */
	private DownloadDispatcher[] mDispatchers;

	/**
	 * The download callback delivery.
	 */
	private DownloadDelivery mDelivery;

	/**
	 * Used for generating monotonically-increasing sequence numbers for requests.
	 */
	private AtomicInteger mSequenceGenerator = new AtomicInteger();

	/**
	 * Default download reuqest queue.
	 */
	public DownloadRequestQueue() {
		this(DEFAULT_DOWNLOAD_THREAD_POOL_SIZE);
	}

	/**
	 * Create the download dispatchers according to pool size.
	 *
	 * @param threadPoolSize thread pool size of download dispatcher
	 */
	public DownloadRequestQueue(int threadPoolSize) {
		if (threadPoolSize < 1 || threadPoolSize > 10) {
			threadPoolSize = DEFAULT_DOWNLOAD_THREAD_POOL_SIZE;
		}

		mDispatchers = new DownloadDispatcher[threadPoolSize];
		mDelivery = new DownloadDelivery(new Handler(Looper.getMainLooper()));
	}

	/**
	 * Starts the dispatchers in this queue.
	 */
	void start() {
		/* make sure any currently running dispatchers are stopped */
		stop();
		
		/* create the download dispatcher and start it. */
		for (int i = 0; i < mDispatchers.length; i++) {
			DownloadDispatcher dispatcher = new DownloadDispatcher(mDownloadQueue, mDelivery);
			mDispatchers[i] = dispatcher;
			dispatcher.start();
		}
	}

	/**
	 * Stops the download dispatchers.
	 */
	void stop() {
		for (DownloadDispatcher dispatcher : mDispatchers) {
			if (dispatcher != null) {
				dispatcher.quit();
			}
		}
	}

	/**
	 * Add download request to the download request queue.
	 *
	 * @param request download request
	 * @return true if the request is not in queue, otherwise return false
	 */
	boolean add(DownloadRequest request) {
		/* if the request is downloading, do nothing */
		if (query(request.downloadId()) != DownloadState.INVALID ||
			query(request.uri().toString()) != DownloadState.INVALID) {
			Log.w(TAG, "the download requst is in downloading");
			return false;
		}

		/* tag the request as belonging to this queue */
		request.setDownloadRequestQueue(this);
		/* add it to the set of current requests */
		synchronized (mCurrentRequests) {
			mCurrentRequests.add(request);
		}

		/* process requests in the order they are added in */
		mDownloadQueue.add(request);

		return true;
	}

	/**
	 * Cancel a download in progress.
	 *
	 * @param downloadId download id
	 * @return true if download has canceled, otherwise return false
	 */
	boolean cancel(int downloadId) {
		synchronized (mCurrentRequests) {
			for (DownloadRequest request : mCurrentRequests) {
				if (request.downloadId() == downloadId) {
					request.cancel();
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Cancel all the download.
	 */
	void cancelAll() {
		synchronized (mCurrentRequests) {
			for (DownloadRequest request : mCurrentRequests) {
				request.cancel();
			}
		}

		mCurrentRequests.clear();
	}

	/**
	 * Get the downloading task size.
	 *
	 * @return task size
	 */
	int getDownloadingSize() {
		return mCurrentRequests.size();
	}

	/**
	 * To check if the request is downloading according to download id.
	 *
	 * @param downloadId download id
	 * @return true if the request is downloading, otherwise return false
	 */
	DownloadState query(int downloadId) {
		synchronized (mCurrentRequests) {
			for (DownloadRequest request : mCurrentRequests) {
				if (request.downloadId() == downloadId) {
					return request.downloadState();
				}
			}
		}

		return DownloadState.INVALID;
	}

	/**
	 * To check if the request is downloading according to download url.
	 *
	 * @param url the url to check
	 * @return true if the request is downloading, otherwise return false
	 */
	DownloadState query(String url) {
		synchronized (mCurrentRequests) {
			for (DownloadRequest request : mCurrentRequests) {
				if (request.uri().toString().equals(url)) {
					return request.downloadState();
				}
			}
		}

		return DownloadState.INVALID;
	}

	/**
	 * Gets a sequence number.
	 *
	 * @return return the sequence number
	 */
	public int getSequenceNumber() {
		return mSequenceGenerator.incrementAndGet();
	}

	/**
	 * The download has finished and remove from set.
	 *
	 * @param request download reqeust
	 */
	void finish(DownloadRequest request) {
		synchronized (mCurrentRequests) {
			mCurrentRequests.remove(request);
		}
	}

	/**
	 * Release all the resource.
	 */
	void release() {
		/* release current download request */
		cancelAll();

		/* release download queue */
		if (mDownloadQueue != null) {
			mDownloadQueue = null;
		}

		/* release dispathcers */
		if (mDispatchers != null) {
			stop();

			for (int i = 0; i < mDispatchers.length; i++) {
				mDispatchers[i] = null;
			}

			mDispatchers = null;
		}
	}
}
