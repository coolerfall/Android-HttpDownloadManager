package com.coolerfall.download;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.coolerfall.download.DownloadRequest.DownloadState;

/**
 * Download request queue, this is designed according to RequestQueue in Andoird-Volley.
 * 
 * @author Vincent Cheung
 * @since  Nov. 24, 2014
 */
public class DownloadRequestQueue {
	private static final String TAG = DownloadRequestQueue.class.getSimpleName();

	/** the capacity of download request queue */
	private static final int CAPACITY = 20;
	
	/** the default number of download dispatcher threads */
	private static final int DEFAULT_DOWNLOAD_THREAD_POOL_SIZE = 3;
	
	/**
	 * The set of all requests currently being processed by this DownloadQueue. A Request
	 * will be in this set if it is waiting in any queue or currently being processed by
	 * any dispatcher.
	 */
	private final Set<DownloadRequest> mCurrentRequests = new HashSet<>();
	
	/** the queue of download request */
	private PriorityBlockingQueue<DownloadRequest> mDownloadQueue = 
			new PriorityBlockingQueue<>(CAPACITY);
	
	/** download dispatchers */
	private DownloadDispatcher[] mDispatchers;
	
	/** download callback delivery */
	private DownloadDelivery mDelivery;

	/** used for generating monotonically-increasing sequence numbers for requests */
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
		if (threadPoolSize < 1 || threadPoolSize > 5) {
			threadPoolSize = DEFAULT_DOWNLOAD_THREAD_POOL_SIZE;
		}
		
		mDispatchers = new DownloadDispatcher[threadPoolSize];
		mDelivery = new DownloadDelivery(new Handler(Looper.getMainLooper()));
	}

	/**
	 * Starts the dispatchers in this queue.
	 */
	protected void start() {
		/* make sure any currently running dispatchers are stopped */
		stop();
		
		/* create the download dispatcher and start it. */
		for (int i = 0; i < mDispatchers.length; i ++) {
			DownloadDispatcher dispatcher = new DownloadDispatcher(mDownloadQueue, mDelivery);
			mDispatchers[i] = dispatcher;
			dispatcher.start();
		}
	}

	/**
	 * Stops the download dispatchers.
	 */
	protected void stop() {
		for (DownloadDispatcher dispatcher : mDispatchers) {
			if (dispatcher != null) {
				dispatcher.quit();
			}
		}
	}

	/**
	 * Add download request to the download request queue.
	 * 
	 * @param  request download request
	 * @return         true if the request is not in queue, otherwise return false
	 */
	protected boolean add(DownloadRequest request) {
		/* check if url is empty */
		if (TextUtils.isEmpty(request.getUrl())) {
			Log.w(TAG, "download url cannot be empty");
			return false;
		}

		/* if the request is downloading, do nothing */
		if (query(request.getDownloadId()) != DownloadState.INVALID ||
			query(request.getUrl()) != DownloadState.INVALID) {
			Log.w(TAG, "the download requst is in downloading");
			return false;
		}

		/* tag the request as belonging to this queue */
		request.setDownloadQueue(this);
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
	 * @param  downloadId download id
	 * @return            true if download has canceled, otherwise return false
	 */
	protected boolean cancel(int downloadId) {
		synchronized (mCurrentRequests) {
			for (DownloadRequest request : mCurrentRequests) {
				if (request.getDownloadId() == downloadId) {
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
	protected void cancelAll() {
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
	protected int getDownloadingSize() {
		return mCurrentRequests.size();
	}

	/**
	 * To check if the request is downloading according to download id.
	 *
	 * @param  downloadId download id
	 * @return            true if the request is downloading, otherwise return false
	 */
	protected DownloadState query(int downloadId) {
		synchronized (mCurrentRequests) {
			for (DownloadRequest request : mCurrentRequests) {
				if (request.getDownloadId() == downloadId) {
					return request.getDownloadState();
				}
			}
		}

		return DownloadState.INVALID;
	}

	/**
	 * To check if the request is downloading according to download url.
	 *
	 * @param  url the url to check
	 * @return     true if the request is downloading, otherwise return false
	 */
	protected DownloadState query(String url) {
		synchronized (mCurrentRequests) {
			for (DownloadRequest request : mCurrentRequests) {
				if (request.getUrl().equals(url)) {
					return request.getDownloadState();
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
	protected void finish(DownloadRequest request) {
		synchronized (mCurrentRequests) {
			mCurrentRequests.remove(request);
		}
	}

	/**
	 * Release all the resource.
	 */
	protected void release() {
		/* release current download request */
		cancelAll();

		/* release download queue */
		if (mDownloadQueue != null) {
			mDownloadQueue = null;
		}

		/* release dispathcers */
		if (mDispatchers != null) {
			stop();

			for (int i = 0; i < mDispatchers.length; i ++) {
				mDispatchers[i] = null;
			}

			mDispatchers = null;
		}
	}
}
