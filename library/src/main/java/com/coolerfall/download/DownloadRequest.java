package com.coolerfall.download;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import static com.coolerfall.download.Utils.createDefaultDownloader;
import static com.coolerfall.download.Preconditions.checkNotNull;

/**
 * This class represents a request for downloading, this is designed according to Request in
 * Andoird-Volley.
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
public final class DownloadRequest implements Comparable<DownloadRequest> {
	private static final String TAG = DownloadRequest.class.getSimpleName();
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

	private int mDownloadId = -1;
	private final AtomicInteger mRetryTime = new AtomicInteger(1);
	private int mAllowedNetworkTypes = 0;
	private final Context mContext;
	private DownloadState mDownloadState = DownloadState.PENDING;
	private final Uri mUri;
	private final String mDestinationDir;
	private String mDestinationFilePath;
	private final int mProgressInterval;
	private DownloadRequestQueue mDownloadRequestQueue;
	private final long mTimestamp = System.currentTimeMillis() / 1000;
	private Priority mPriority = Priority.NORMAL;
	private boolean mCanceled = false;
	private Downloader mDownloader;
	private DownloadCallback mDownloadCallback;

	private DownloadRequest(Builder builder) {
		if (builder.retryTime != 0) {
			mRetryTime.set(builder.retryTime);
		}
		checkNotNull(builder.context, "context == null");
		mContext = builder.context.getApplicationContext();
		mUri = Uri.parse(checkNotNull(builder.url, "url == null"));
		mDestinationDir = builder.destinationDir;
		mDestinationFilePath = builder.destinationFilePath;
		mProgressInterval = builder.progressInterval;
		mAllowedNetworkTypes = builder.allowedNetworkTypes;
		mDownloadCallback = builder.downloadCallback;
	}

	@Override public int compareTo(@NonNull DownloadRequest other) {
		Priority left = this.priority();
		Priority right = other.priority();
		
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
	Priority priority() {
		return mPriority;
	}

	/**
	 * Get {@link Downloader} to use.
	 *
	 * @return {@link Downloader}
	 */
	Downloader downloader() {
		if (mDownloader == null) {
			mDownloader = createDefaultDownloader();
		}

		return mDownloader;
	}

	/**
	 * Set a downloader for current reqeust to use.
	 *
	 * @param downloader {@link Downloader}
	 */
	void setDownloader(Downloader downloader) {
		mDownloader = downloader;
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
	 */
	void setDownloadRequestQueue(DownloadRequestQueue queue) {
		mDownloadRequestQueue = queue;
		mDownloadId = mDownloadRequestQueue.getSequenceNumber();
	}

	/**
	 * Update the {@link DownloadState} of current download request.
	 *
	 * @param downloadState {@link DownloadState}
	 */
	void updateDownloadState(DownloadState downloadState) {
		mDownloadState = downloadState;
	}

	/**
	 * Get download state of current request.
	 *
	 * @return download state
	 */
	DownloadState downloadState() {
		return mDownloadState;
	}

	/**
	 * Get the download id of this download request.
	 *
	 * @return download id
	 */
	int downloadId() {
		return mDownloadId;
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
	String getFilePath() {
		String dir = TextUtils.isEmpty(mDestinationDir) ? DEFAULT_DIR : mDestinationDir;
		return dir + File.separator + Utils.getFilenameFromHeader(mUri.toString());
	}

	/**
	 * Get destination file path of this download request.
	 *
	 * @return destination file path
	 */
	@SuppressWarnings("ResultOfMethodCallIgnored") String destinationFilePath() {
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
		return destinationFilePath() + ".tmp";
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
		private int retryTime;
		private Context context;
		private String url;
		private String destinationDir;
		private String destinationFilePath;
		private int progressInterval;
		private int allowedNetworkTypes;
		private DownloadCallback downloadCallback;

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

		public Builder downloadCallback(DownloadCallback downloadCallback) {
			this.downloadCallback = downloadCallback;
			return this;
		}

		public DownloadRequest build() {
			return new DownloadRequest(this);
		}
	}
}
