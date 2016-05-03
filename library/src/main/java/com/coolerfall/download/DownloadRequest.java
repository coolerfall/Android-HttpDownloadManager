package com.coolerfall.download;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import static com.coolerfall.download.Utils.createDefaultDownloader;
import static com.coolerfall.download.Preconditions.checkNotNull;

/**
 * DownloadRequest: download request, this is designed according to Request in Andoird-Volley.
 *
 * @author Vincent Cheung
 * @since Nov. 24, 2014
 */
public final class DownloadRequest implements Comparable<DownloadRequest> {
	private static final String TAG = DownloadRequest.class.getSimpleName();

	/**
	 * default download directory
	 */
	private static final String DEFAULT_DIR = Environment
		.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
		.getAbsolutePath();

	/**
	 * Bit flag corresponding to {@link ConnectivityManager#TYPE_MOBILE}.
	 */
	public static final int NETWORK_MOBILE = 1;

	/**
	 * Bit flag corresponding to {@link ConnectivityManager#TYPE_WIFI}.
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
	private DownloadState mDownloadState = DownloadState.PENDING;

	/**
	 * Uri of download request.
	 */
	private Uri mUri;

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

	private Downloader mDownloader;

	/**
	 * Download callback.
	 */
	private DownloadCallback mDownloadCallback;

	private DownloadRequest(Builder builder) {
		mDownloadId = builder.downloadId;
		if (builder.retryTime != 0) {
			mRetryTime = new AtomicInteger(builder.retryTime);
		}
		mContext = checkNotNull(builder.context, "context == null");
		mUri = Uri.parse(checkNotNull(builder.url, "url == null"));
		mDestinationDir = builder.destinationDir;
		mDestinationFilePath = builder.destinationFilePath;
		mProgressInterval = builder.progressInterval;
		mAllowedNetworkTypes = builder.allowedNetworkTypes;
		mDownloader = builder.downloader;
		if (mDownloader == null) {
			mDownloader = createDefaultDownloader();
		}
		mDownloadCallback = builder.downloadCallback;
	}

	@Override public int compareTo(DownloadRequest other) {
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
	 * Get the priority of download request.
	 *
	 * @return {@link Priority#NORMAL} by default.
	 */
	Priority getPriority() {
		return mPriority;
	}

	/**
	 * Get {@link Downloader} to use.
	 *
	 * @return {@link Downloader}
	 */
	Downloader downloader() {
		return mDownloader;
	}

	/**
	 * Get the download callback of this request.
	 *
	 * @return download callback
	 */
	DownloadCallback downloadCallback() {
		return mDownloadCallback;
	}

	/**
	 * Associates this request with the given queue. The request queue will be
	 * notified when this request has finished.
	 *
	 * @param queue download request queue
	 * @return this Request object to allow for chaining
	 */
	DownloadRequest setDownloadQueue(DownloadRequestQueue queue) {
		mDownloadRequestQueue = queue;

		return this;
	}

	/**
	 * Set download state of this request.
	 *
	 * @param state download state
	 */
	void setDownloadState(DownloadState state) {
		mDownloadState = state;
	}

	/**
	 * Get download state of current request.
	 *
	 * @return download state
	 */
	DownloadState getDownloadState() {
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
	 * Get retry time, the retry time will decrease automatically after invoking this method.
	 *
	 * @return retry time
	 */
	int retryTime() {
		return mRetryTime.decrementAndGet();
	}

	/**
	 * Get progress interval, used in {@link DownloadDispatcher}.
	 *
	 * @return progress interval
	 */
	int progressInterval() {
		return mProgressInterval;
	}

	/**
	 * Get the types of allowed network.
	 *
	 * @return all the types
	 */
	int allowedNetworkTypes() {
		return mAllowedNetworkTypes;
	}

	/**
	 * Get the context.
	 *
	 * @return context
	 */
	Context context() {
		return mContext;
	}

	/**
	 * Get the URL of this request.
	 *
	 * @return the URL of this request
	 */
	Uri uri() {
		return mUri;
	}

	/* get absolute file path according to the directory */
	private String getFilePath() {
		String dir = TextUtils.isEmpty(mDestinationDir) ? DEFAULT_DIR : mDestinationDir;
		return dir + File.separator + Utils.getFilenameFromHeader(mUri.toString());
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
	@SuppressWarnings("ResultOfMethodCallIgnored") String getDestFilePath() {
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
	String tempFilePath() {
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
		private int downloadId;
		private int retryTime;
		private Context context;
		private String url;
		private String destinationDir;
		private String destinationFilePath;
		private int progressInterval;
		private int allowedNetworkTypes;
		private Downloader downloader;
		private DownloadCallback downloadCallback;

		public Builder downloadId(int downloadId) {
			this.downloadId = downloadId;
			return this;
		}

		public Builder retryTime(int retryTime) {
			this.retryTime = retryTime;
			return this;
		}

		public Builder context(Context context) {
			this.context = context;
			return this;
		}

		public Builder url(String url) {
			this.url = url;
			return this;
		}

		public Builder destinationDir(String destinationDir) {
			this.destinationDir = destinationDir;
			return this;
		}

		public Builder destinationFilePath(String destinationFilePath) {
			this.destinationFilePath = destinationFilePath;
			return this;
		}

		public Builder progressInterval(int progressInterval) {
			this.progressInterval = progressInterval;
			return this;
		}

		public Builder allowedNetworkTypes(int allowedNetworkTypes) {
			this.allowedNetworkTypes = allowedNetworkTypes;
			return this;
		}

		public Builder downloader(Downloader downloader) {
			this.downloader = downloader;
			return this;
		}

		public Builder downloadCallback(DownloadCallback downloadCallback) {
			this.downloadCallback = downloadCallback;
			return this;
		}

		public DownloadRequest build() {
			return new DownloadRequest(this);
		}
	}
}
