package com.coolerfall.download;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.coolerfall.download.Preconditions.checkNotNull;

/**
 * This class represents a request for downloading, this is designed according to Request in
 * Andoird-Volley.
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
public final class DownloadRequest implements Comparable<DownloadRequest> {
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

	private int downloadId = -1;
	private final AtomicInteger retryTime;
	private int allowedNetworkTypes = 0;
	private Context context;
	private DownloadState downloadState;
	private final Uri uri;
	private final String destinationDirectory;
	private String destinationFilePath;
	private final long progressInterval;
	private DownloadRequestQueue downloadRequestQueue;
	private final long timestamp;
	private final Priority priority;
	private boolean canceled = false;
	private Downloader downloader;
	private final DownloadCallback downloadCallback;

	private DownloadRequest(Builder builder) {
		priority = checkNotNull(builder.priority, "priority == null");
		uri = Uri.parse(checkNotNull(builder.url, "url == null"));
		retryTime = new AtomicInteger(builder.retryTime);
		destinationDirectory =
			checkNotNull(builder.destinationDirectory, "destinationDirectory == null");
		destinationFilePath = builder.destinationFilePath;
		downloadCallback = checkNotNull(builder.downloadCallback, "downloadCallback == null");
		progressInterval = builder.progressInterval;
		allowedNetworkTypes = builder.allowedNetworkTypes;
		downloadState = DownloadState.PENDING;
		timestamp = System.currentTimeMillis();
	}

	@Override public int compareTo(@NonNull DownloadRequest other) {
		Priority left = this.priority();
		Priority right = other.priority();
		
		/*
		 * High-priority requests are "lesser" so they are sorted to the front.
		 * Equal priorities are sorted by timestamp to provide FIFO ordering.
		 */
		return left == right ?
			(int) (this.timestamp - other.timestamp) :
			right.ordinal() - left.ordinal();
	}

	/**
	 * Get the priority of download request.
	 *
	 * @return {@link Priority#NORMAL} by default.
	 */
	Priority priority() {
		return priority;
	}

	/**
	 * Get {@link Downloader} to use.
	 *
	 * @return {@link Downloader}
	 */
	Downloader downloader() {
		return downloader;
	}

	/**
	 * Set a downloader for current reqeust to use.
	 *
	 * @param downloader {@link Downloader}
	 */
	void setDownloader(Downloader downloader) {
		this.downloader = downloader;
	}

	/**
	 * Get the download callback of this request.
	 *
	 * @return download callback
	 */
	DownloadCallback downloadCallback() {
		return downloadCallback;
	}

	/**
	 * Associates this request with the given queue. The request queue will be
	 * notified when this request has finished.
	 *
	 * @param queue download request queue
	 */
	void setDownloadRequestQueue(DownloadRequestQueue queue) {
		downloadRequestQueue = queue;
		downloadId = downloadRequestQueue.getSequenceNumber();
	}

	/**
	 * Update the {@link DownloadState} of current download request.
	 *
	 * @param downloadState {@link DownloadState}
	 */
	void updateDownloadState(DownloadState downloadState) {
		this.downloadState = downloadState;
	}

	/**
	 * Get download state of current request.
	 *
	 * @return download state
	 */
	DownloadState downloadState() {
		return downloadState;
	}

	/**
	 * Get the download id of this download request.
	 *
	 * @return download id
	 */
	int downloadId() {
		return downloadId;
	}

	/**
	 * Get retry time, the retry time will decrease automatically after invoking this method.
	 *
	 * @return retry time
	 */
	int retryTime() {
		return retryTime.decrementAndGet();
	}

	/**
	 * Get progress interval, used in {@link DownloadDispatcher}.
	 *
	 * @return progress interval
	 */
	long progressInterval() {
		return progressInterval;
	}

	/**
	 * Get the types of allowed network.
	 *
	 * @return all the types
	 */
	int allowedNetworkTypes() {
		return allowedNetworkTypes;
	}

	/**
	 * Set context to use.
	 *
	 * @param context context
	 */
	void setContext(Context context) {
		this.context = context;
	}

	/**
	 * Get the context.
	 *
	 * @return context
	 */
	Context context() {
		return context;
	}

	/**
	 * Get the URL of this request.
	 *
	 * @return the URL of this request
	 */
	Uri uri() {
		return uri;
	}

	/**
	 * Get absolute file path according to the directory
	 *
	 * @return file path to save file
	 */
	String getFilePath() {
		return destinationDirectory + File.separator + Utils.getFilenameFromHeader(uri.toString());
	}

	/**
	 * Get destination file path of this download request.
	 *
	 * @return destination file path
	 */
	@SuppressWarnings("ResultOfMethodCallIgnored") String destinationFilePath() {
		/* if the destination file path is empty, use default file path */
		if (TextUtils.isEmpty(destinationFilePath)) {
			destinationFilePath = getFilePath();
		}

		/* if the destination path is directory */
		File file = new File(destinationFilePath);
		if (!file.getParentFile().exists()) {
			/* make dirs in case */
			file.getParentFile().mkdirs();
		}

		return destinationFilePath;
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
		canceled = true;
	}

	/**
	 * To check if current request has canceled.
	 *
	 * @return Returns true if this request has been canceled.
	 */
	boolean isCanceled() {
		return canceled;
	}

	/**
	 * Notifies the download request queue that this request has finished(succesfully or fail)
	 */
	void finish() {
		if (downloadRequestQueue != null) {
			downloadRequestQueue.finish(this);
		}
	}

	public static final class Builder {
		private int retryTime;
		private String url;
		private String destinationDirectory;
		private String destinationFilePath;
		private Priority priority;
		private long progressInterval;
		private int allowedNetworkTypes;
		private DownloadCallback downloadCallback;

		public Builder() {
			this.retryTime = 1;
			this.priority = Priority.NORMAL;
			this.destinationDirectory = DEFAULT_DIR;
			this.downloadCallback = DownloadCallback.EMPTY_CALLBACK;
		}

		public Builder retryTime(int retryTime) {
			if (retryTime < 0) {
				throw new IllegalArgumentException("retryTime < 0");
			}

			this.retryTime = retryTime;
			return this;
		}

		public Builder url(String url) {
			this.url = url;
			return this;
		}

		public Builder destinationDirectory(String destinationDirectory) {
			this.destinationDirectory = destinationDirectory;
			return this;
		}

		public Builder destinationFilePath(String destinationFilePath) {
			/* if the destination path is directory */
			if (new File(destinationFilePath).isDirectory()) {
				throw new IllegalArgumentException("destinationFilePath cannot be a directory");
			}
			this.destinationFilePath = destinationFilePath;
			return this;
		}

		public Builder priority(Priority priority) {
			this.priority = priority;
			return this;
		}

		public Builder progressInterval(long interval, TimeUnit unit) {
			if (interval < 0) {
				throw new IllegalArgumentException("interval < 0");
			}

			if (unit == null) {
				throw new NullPointerException("unit == null");
			}

			long millis = unit.toMillis(interval);
			if (millis > Integer.MAX_VALUE) {
				throw new IllegalArgumentException("interval too large");
			}

			this.progressInterval = millis;
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
