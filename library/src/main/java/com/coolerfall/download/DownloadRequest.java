package com.coolerfall.download;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.net.HttpURLConnection;

import javax.net.ssl.HttpsURLConnection;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * DownloadRequest: download request, this is designed according to Request in Andoird-Volley.
 *
 * @author Vincent Cheung
 * @since Nov. 24, 2014
 */
public class DownloadRequest implements Comparable<DownloadRequest> {
	private static final String TAG = DownloadRequest.class.getSimpleName();

	/**
	 * default download directory
	 */
	private static final String DEFAULT_DIR = Environment
		.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
		.getAbsolutePath();

	/**
	 * Bit flag for {@link #setAllowedNetworkTypes} corresponding to
	 * {@link ConnectivityManager#TYPE_MOBILE}.
	 */
	public static final int NETWORK_MOBILE = 1;

	/**
	 * Bit flag for {@link #setAllowedNetworkTypes} corresponding to
	 * {@link ConnectivityManager#TYPE_WIFI}.
	 */
	public static final int NETWORK_WIFI = 1 << 1;

	/**
	 * Download id of this download request.
	 */
	private int mDownloadId = -1;

	/**
	 * Retry time when downloading failed, default is 1.
	 */
	private AtomicInteger mRetryTime = new AtomicInteger(1);

	/**
	 * Allowed network types, default to all network types allowed.
	 */
	private int mAllowedNetworkTypes = 0;

	/**
	 * The context used in {@link DownloadDispatcher}.
	 */
	private Context mContext;

	/**
	 * The download state.
	 */
	private DownloadState mDownloadState;

	/**
	 * URL of download request.
	 */
	private String mUrl;

	/**
	 * Custom {@link HttpURLConnection} or {@link HttpsURLConnection}.
	 */
	private HttpURLConnection mHttpURLConnection;

	/**
	 * Destination directory to save file.
	 */
	private String mDestinationDir;

	/**
	 * Destination file path.
	 */
	private String mDestinationFilePath;

	/**
	 * Progress interval, how long should {@link DownloadDispatcher}
	 * invoke {@link DownloadCallback#onProgress(int, long, long)}.
	 */
	private int mProgressInterval;

	/**
	 * Download request queue.
	 */
	private DownloadRequestQueue mDownloadRequestQueue;

	/**
	 * Timestamp of this download request when created.
	 */
	private long mTimestamp = System.currentTimeMillis() / 1000;

	/**
	 * The priority of this download request, normal by default.
	 */
	private Priority mPriority = Priority.NORMAL;

	/**
	 * Whether or not this request has been canceled.
	 */
	private boolean mCanceled = false;

	/**
	 * Download listener.
	 */
	private DownloadCallback mDownloadCallback;

	private DownloadRequest(Builder builder) {
		mRetryTime = builder.mRetryTime;
		mContext = builder.mContext;
		mUrl = builder.mUrl;
		mDestinationDir = builder.mDestinationDir;
		mDestinationFilePath = builder.mDestinationFilePath;
		mProgressInterval = builder.mProgressInterval;
		mDownloadCallback = builder.mDownloadCallback;
	}

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
	 * @param priority {@link Priority}
	 * @return this Request object to allow for chaining
	 */
	public DownloadRequest setPriority(Priority priority) {
		mPriority = priority;

		return this;
	}

	/**
	 * Get the priority of download request.
	 *
	 * @return {@link Priority#NORMAL} by default.
	 */
	protected Priority getPriority() {
		return mPriority;
	}

	/**
	 * Set the download callback.
	 *
	 * @param cb download callback
	 * @return this Request object to allow for chaining
	 */
	public DownloadRequest setDownloadCallback(DownloadCallback cb) {
		mDownloadCallback = cb;

		return this;
	}

	/**
	 * Get the download callback of this request.
	 *
	 * @return download callback
	 */
	protected DownloadCallback getDownloadCallback() {
		return mDownloadCallback;
	}

	/**
	 * Associates this request with the given queue. The request queue will be
	 * notified when this request has finished.
	 *
	 * @param queue download request queue
	 * @return this Request object to allow for chaining
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
	 * @param downloadId download id
	 * @return download request
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
	 * @param retryTime retry time
	 * @return this Request object to allow for chaining
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
	 * Set progress interval for this download request.
	 *
	 * @param millisec interval in millisecond
	 * @return this Request object to allow for chaining
	 */
	public DownloadRequest setProgressInterval(int millisec) {
		mProgressInterval = millisec;
		return this;
	}

	/**
	 * Get progress interval, used in {@link DownloadDispatcher}.
	 *
	 * @return progress interval
	 */
	protected int getProgressInterval() {
		return mProgressInterval;
	}

	/**
	 * Restrict the types of networks over which this download may proceed.
	 * By default, all network types are allowed.
	 * Be sure to add permission android.permission.ACCESS_NETWORK_STATE.
	 *
	 * @param context the context to use
	 * @param types   any network type
	 * @return this Request object to allow for chaining
	 */
	public DownloadRequest setAllowedNetworkTypes(Context context, int types) {
		mContext = context.getApplicationContext();
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
	 * Set the url of this download request.
	 *
	 * @param url the url
	 * @return this Request object to allow for chaining
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

	/**
	 * Set custom {@link HttpURLConnection} or {@link HttpsURLConnection}.
	 *
	 * @param conn http connection
	 * @return this Request object to allow for chaining
	 */
	public DownloadRequest setHttpURLConnection(HttpURLConnection conn) {
		mHttpURLConnection = conn;

		return this;
	}

	/**
	 * Get the custom {@link HttpURLConnection} or {@link HttpsURLConnection}.
	 *
	 * @return http connection
	 */
	protected HttpURLConnection getHttpURLConnection() {
		return mHttpURLConnection;
	}

	/* get absolute file path according to the directory */
	private String getFilePath() {
		String dir = TextUtils.isEmpty(mDestinationDir) ? DEFAULT_DIR : mDestinationDir;
		return dir + File.separator + DownloadUtils.getFilenameFromHeader(mUrl);
	}

	/**
	 * Set destination file path of this download request. The file will be createad
	 * according to the file path. This file path must be absolute file
	 * path(such as: /sdcard/test.txt). If the filename is not certain, then use
	 * {@link #setDestDirectory(String)}, the download manager will genrate filename from url.
	 *
	 * @param filePath destination file path
	 * @return this Request object to allow for chaining
	 * @see #setDestDirectory(String)
	 */
	public DownloadRequest setDestFilePath(String filePath) {
		mDestinationFilePath = filePath;
		return this;
	}

	/**
	 * Set absolute destination directory for this download request.
	 * If {@link #setDestFilePath(String)} was used, then destination directory will
	 * be ignored. The directory will be created if not existed.
	 * The name of file will be generated from url or http header.
	 *
	 * @param dir destination directory
	 * @return this Request object to allow for chaining
	 * @see #setDestFilePath(String)
	 */
	public DownloadRequest setDestDirectory(String dir) {
		mDestinationDir = dir;
		return this;
	}

	/**
	 * Get destination file path of this download request.
	 *
	 * @return destination file path
	 */
	@SuppressWarnings("ResultOfMethodCallIgnored")
	String getDestFilePath() {
		/* if the destination file path is empty, use default file path */
		if (TextUtils.isEmpty(mDestinationFilePath)) {
			mDestinationFilePath = getFilePath();
		}

		/* if the destination path is directory */
		File file = new File(mDestinationFilePath);
		if (file.isDirectory()) {
			Log.w(TAG, "the destination file path cannot be directory");
			return getFilePath();
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
	String getTmpDestinationPath() {
		return getDestFilePath() + ".tmp";
	}

	/**
	 * Mark this download request as canceled. No callback will be delivered.
	 */
	void cancel() {
		mCanceled = true;
	}

	/**
	 * To check if current request has canceled.
	 *
	 * @return Returns true if this request has been canceled.
	 */
	boolean isCanceled() {
		return mCanceled;
	}

	/**
	 * Notifies the download request queue that this request has finished(succesfully or fail)
	 */
	void finish() {
		if (mDownloadRequestQueue != null) {
			mDownloadRequestQueue.finish(this);
		}
	}

	public static final class Builder {
		private AtomicInteger mRetryTime;
		private Context mContext;
		private String mUrl;
		private String mDestinationDir;
		private String mDestinationFilePath;
		private int mProgressInterval;
		private DownloadCallback mDownloadCallback;

		public Builder() {
		}

		public Builder mRetryTime(AtomicInteger val) {
			mRetryTime = val;
			return this;
		}

		public Builder mContext(Context val) {
			mContext = val;
			return this;
		}

		public Builder mUrl(String val) {
			mUrl = val;
			return this;
		}

		public Builder mDestinationDir(String val) {
			mDestinationDir = val;
			return this;
		}

		public Builder mDestinationFilePath(String val) {
			mDestinationFilePath = val;
			return this;
		}

		public Builder mProgressInterval(int val) {
			mProgressInterval = val;
			return this;
		}

		public Builder mDownloadCallback(DownloadCallback val) {
			mDownloadCallback = val;
			return this;
		}

		public DownloadRequest build() {
			return new DownloadRequest(this);
		}
	}
}
