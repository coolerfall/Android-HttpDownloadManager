package com.coolerfall.download;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DownloadRequest: download request, this is designed according to Request in Andoird-Volley.
 * 
 * @author Vincent Cheung
 * @since  Nov. 24, 2014
 */
public class DownloadRequest implements Comparable<DownloadRequest> {
	private static final String TAG = DownloadRequest.class.getSimpleName();

	/** default download directory */
	private static final String DEFAULT_DIR = Environment
			.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
			.getAbsolutePath();

	/**
	 * Bit flag for {@link #setAllowedNetworkTypes} corresponding to
	 * {@link android.net.ConnectivityManager#TYPE_MOBILE}.
	 */
	public static final int NETWORK_MOBILE = 1 << 0;

	/**
	 * Bit flag for {@link #setAllowedNetworkTypes} corresponding to
	 * {@link android.net.ConnectivityManager#TYPE_WIFI}.
	 */
	public static final int NETWORK_WIFI = 1 << 1;

	/** download id of this download request */
	private int mDownloadId = -1;
	
	/** retry time when downloading failed, default is 1 */
	private AtomicInteger mRetryTime = new AtomicInteger(1);

	/** allowed network types, default to all network types allowed */
	private int mAllowedNetworkTypes = 0;

	/** the context used in {@link com.coolerfall.download.DownloadDispatcher} */
	private Context mContext;

	/** the download state */
	private DownloadState mDownloadState;
	
	/** URL of download request */
	private String mUrl;
	
	/** destination file path */
	private String mDestinationFilePath;
	
	/** download request queue */
	private DownloadRequestQueue mDownloadRequestQueue;
	
	/** timestamp of this download request when created */
	private long mTimestamp = System.currentTimeMillis() / 1000;
	
	/** the priority of this download request, normal by default */
	private Priority mPriority = Priority.NORMAL;
	
	/** Whether or not this request has been canceled. */
	private boolean mCanceled = false;
    
	/** download listener */
	private DownloadListener mDownloadListener;

	/** custom headers to use for this request **/
	private HashMap<String, String> mHeaders;

	/**
	 * Priority values: download request will be processed from 
	 * higher priorities to lower priorities.
	 */
	public enum Priority {
		/**
		 * The lowest priority.
		 */
		LOW,
		/**
		 * Normal priority(default).
		 */
		NORMAL,
		/**
		 * The highest priority.
		 */
		HIGH,
	}
	
	/**
	 * State valuse: this will used to mark the state of download request.
	 */
	protected enum DownloadState {
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
	
	/**
	 * The default constructor, set the download state as pending.
	 */
	public DownloadRequest() {
		mDownloadState = DownloadState.PENDING;
		mHeaders  = new HashMap<String, String>();
	}
	
	@Override
	public int compareTo(DownloadRequest other) {
		Priority left = this.getPriority();
		Priority right = other.getPriority();
		
		/*
		 * High-priority requests are "lesser" so they are sorted to the front.
		 * Equal priorities are sorted by timestamp to provide FIFO ordering.
		 */
		return left == right ? 
				(int) (this.mTimestamp - other.mTimestamp) : 
				right.ordinal() - left.ordinal();
	}
	
	/**
	 * Set the priority of this downloader.
	 * 
	 * @param  priority {@link Priority}
	 * @return          this Request object to allow for chaining
	 */
	public DownloadRequest setPriority(Priority priority) {
		mPriority = priority;
		
		return this;
	}


	/** Add a header to this download request
	 *
	 * @param headerName
	 * @param headerValue
	 * @return this Request object to allow for chaining
	 */
	public DownloadRequest addHeader(String headerName, String headerValue) {
		mHeaders.put(headerName, headerValue);
		return this;
	}

	/** Gets a HashMap of the headers for this download request
	 *
	 * @return HashMap of user-provided header keys and values
	 */
	HashMap<String, String> getHeaders() {
		return mHeaders;
	}

	/**
	 * Get the priority of download request.
	 * 
	 * @return {@link Priority},
	 *         {@link Priority#NORMAL} by default.
	 */
	protected Priority getPriority() {
		return mPriority;
	}
	
	/**
	 * Set the download listener.
	 * 
	 * @param  l download listener
	 * @return   this Request object to allow for chaining
	 */
	public DownloadRequest setDownloadListener(DownloadListener l) {
		mDownloadListener = l;
		
		return this;
	}
	
	/**
	 * Get the download listener of this request.
	 * 
	 * @return download listener
	 */
	protected DownloadListener getDownloadListener() {
		return mDownloadListener;
	}
	
	/**
     * Associates this request with the given queue. The request queue will be 
     * notified when this request has finished.
     *
     * @param  queue download request queue
     * @return       this Request object to allow for chaining
     */
	protected DownloadRequest setDownloadQueue(DownloadRequestQueue queue) {
		mDownloadRequestQueue = queue;
		
		return this;
	}
	
	/**
	 * Set download state of this request.
	 * 
	 * @param state download state
	 */
	protected void setDownloadState(DownloadState state) {
		mDownloadState = state;
	}
	
	/**
	 * Get download state of current request.
	 * 
	 * @return download state
	 */
	protected DownloadState getDownloadState() {
		return mDownloadState;
	}
	
	/**
	 * Set download id of this download request.
	 * 
	 * @param  downloadId download id
	 * @return            download request
	 */
	public DownloadRequest setDownloadId(int downloadId) {
		mDownloadId = downloadId;
		
		return this;
	}
	
	/**
	 * Get the download id of this download request.
	 * 
	 * @return download id
	 */
	protected int getDownloadId() {
		return mDownloadId;
	}
	
	/**
	 * Set retry time, the manager will re-download with retry time.
	 * 
	 * @param  retryTime retry time
	 * @return           this Request object to allow for chaining
	 */
	public DownloadRequest setRetryTime(int retryTime) {
		mRetryTime = new AtomicInteger(retryTime);
		
		return this;
	}
	
	/**
	 * Get retry time, the retry time will decrease automatically 
	 * after invoking this method.
	 * 
	 * @return retry time
	 */
	protected int getRetryTime() {
		return mRetryTime.decrementAndGet();
	}

	/**
	 * Restrict the types of networks over which this download may proceed.
	 * By default, all network types are allowed.
	 * Be sure to add permission android.permission.ACCESS_NETWORK_STATE.
	 *
	 * @param  context context
	 * @param  types   any network type
	 * @return         this Request object to allow for chaining
	 */
	public DownloadRequest setAllowedNetworkTypes(Context context, int types) {
		mContext = context;
		mAllowedNetworkTypes = types;

		return this;
	}

	/**
	 * Get the types of allowed network.
	 *
	 * @return all the types
	 */
	protected int getAllowedNetworkTypes() {
		return mAllowedNetworkTypes;
	}

	/**
	 * Get the context.
	 *
	 * @return context
	 */
	protected Context getContext() {
		return mContext;
	}

	/**
	 * Set the URL of this download request.
	 * 
	 * @param  url the url
	 * @return     this Request object to allow for chaining.
	 */
	public DownloadRequest setUrl(String url) {
		if (TextUtils.isEmpty(url)) {
			throw new IllegalArgumentException("url cannot be null");
		}
		
		if (!url.startsWith("http") && !url.startsWith("https")) {
			throw new IllegalArgumentException("can only download 'HTTP/HTTPS' url");
		}
		
		mUrl = url;
		
		return this;
	}
	
	/**
	 * Get the URL of this request.
	 * 
	 * @return the URL of this request
	 */
	protected String getUrl() {
		return mUrl;
	}

	/** get the default download file path */
	private String getDefaultFilePath() {
		String filename = mTimestamp + ".down";
		int index = mUrl.lastIndexOf(File.separator);
		if (index > 0 && mUrl.length() >= index) {
			filename = mUrl.substring(index);
		}

		//TODO: to get real filename for the sepecified url according to headers

		return DEFAULT_DIR + File.separator + filename;
	}

	/**
	 * Set destination file path of this download request. This method has deprecated,
	 * use {@link #setDestFilePath} instead.
	 * 
	 * @return download request
	 */
	@Deprecated
	public DownloadRequest setDestinationPath(String filePath) {
		mDestinationFilePath = filePath;
		
		return this;
	}

	/**
	 * Set destination file path of this download request. The file will be createad
	 * according to the file path. This file path must be absolute file
	 * path(such as: /sdcard/test.txt).
	 *
	 * @return  this Request object to allow for chaining.
	 */
	public DownloadRequest setDestFilePath(String filePath) {
		mDestinationFilePath = filePath;

		return this;
	}
	
	/**
	 * Get destination file path of this download request.
	 * 
	 * @return destination file path
	 */
	@SuppressWarnings("ResultOfMethodCallIgnored")
	protected String getDestFilePath() {
		/* if the destination file path is empty, use default file path */
		if (TextUtils.isEmpty(mDestinationFilePath)) {
			Log.w(TAG, "the destination file path should not be empty");
			mDestinationFilePath = getDefaultFilePath();
		}

		/* if the destination path is directory */
		File file = new File(mDestinationFilePath);
		if (file.isDirectory()) {
			Log.w(TAG, "the destination file path cannot be directory");
			return getDefaultFilePath();
		} else if (!file.getParentFile().exists()) {
			/* make dirs in case */
			file.getParentFile().mkdirs();
		}

		return mDestinationFilePath;
	}

	/**
	 * Get temporary destination file path of this download request.
	 *
	 * @return temporary destination file path
	 */
	protected String getTmpDestinationPath() {
		return getDestFilePath() + ".tmp";
	}
	
	/**
	 * Mark this download request as canceled.  No callback will be delivered.
	 */
	protected void cancel() {
		mCanceled = true;
	}
	
	/**
     * Returns true if this request has been canceled.
     */
	protected boolean isCanceled() {
		return mCanceled;
	}
	
	/**
	 * Notifies the download request queue that this request has finished(succesfully or fail)
	 */
	protected void finish() {
		if (mDownloadRequestQueue != null) {
			mDownloadRequestQueue.finish(this);
		}
	}
}
