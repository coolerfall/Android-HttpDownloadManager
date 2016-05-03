package com.coolerfall.download;

import android.os.Process;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.concurrent.BlockingQueue;

import static com.coolerfall.download.DownloadManager.HTTP_ERROR_SIZE;
import static com.coolerfall.download.DownloadManager.HTTP_INVALID;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_PARTIAL;

/**
 * Download dispatcher: used to dispatch downloader, this is desinged according to
 * NetworkDispatcher in Android-Volley.
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
final class DownloadDispatcher extends Thread {
	private static final String TAG = DownloadDispatcher.class.getSimpleName();

	/**
	 * Sleep time before download.
	 */
	private static final int SLEEP_BEFORE_DOWNLOAD = 1500;

	/**
	 * Sleep time before retrying download.
	 */
	private static final int SLEEP_BEFORE_RETRY = 3500;

	/**
	 * Buffer size used in data tranfering.
	 */
	private static final int BUFFER_SIZE = 4096;

	/**
	 * End of input stream.
	 */
	private static final String END_OF_STREAM = "unexpected end of stream";

	/**
	 * Default thread name.
	 */
	private static final String DEFAULT_THREAD_NAME = "DownloadDispatcher";

	/**
	 * Idle thread name.
	 */
	private static final String IDLE_THREAD_NAME = "DownloadDispatcher-Idle";

	/**
	 * The queue of download reuqests.
	 */
	private BlockingQueue<DownloadRequest> mQueue;

	/**
	 * To deliver callback on main thread.
	 */
	private DownloadDelivery mDelivery;

	/**
	 * Save total bytes in case.
	 */
	private long mTotalBytes = 0;

	/**
	 * Used to save last progress timestamp.
	 */
	private long mLastProgressTimestamp;

	/**
	 * Used to tell us this dispatcher has dead.
	 */
	private volatile boolean mQuit = false;

	/**
	 * Default constructor, with queue and delivery.
	 *
	 * @param queue    download request queue
	 * @param delivery download delivery
	 */
	public DownloadDispatcher(BlockingQueue<DownloadRequest> queue, DownloadDelivery delivery) {
		mQueue = queue;
		mDelivery = delivery;
		
		/* set thread name to idle */
		setName(IDLE_THREAD_NAME);
	}

	@Override
	public void run() {
		Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
		DownloadRequest request = null;

		while (true) {
			try {
				setName(IDLE_THREAD_NAME);
				request = mQueue.take();
				sleep(SLEEP_BEFORE_DOWNLOAD);
				setName(DEFAULT_THREAD_NAME);

				mTotalBytes = 0;

				/* start download */
				executeDownload(request);
			} catch (InterruptedException e) {
				/* we may have been interrupted because it was time to quit */
				if (mQuit) {
					if (request != null) {
						request.finish();
					}

					return;
				}
			}
		}
	}

	/* update download state */
	private void updateState(DownloadRequest request, DownloadState state) {
		request.setDownloadState(state);
	}

	/* update download start state */
	private void updateStart(DownloadRequest request, long totalBytes) {
		/* if the request has failed before, donnot deliver callback */
		if (request.getDownloadState() == DownloadState.FAILURE) {
			updateState(request, DownloadState.RUNNING);
			return;
		}
		
		/* set the download state of this request as running */
		updateState(request, DownloadState.RUNNING);
		mDelivery.postStart(request, totalBytes);
	}

	/* update download retrying */
	private void updateRetry(DownloadRequest request) {
		mDelivery.postRetry(request);
	}

	/* update download progress */
	private void updateProgress(DownloadRequest request, long bytesWritten, long totalBytes) {
		long currentTimestamp = System.currentTimeMillis();
		if (bytesWritten != totalBytes &&
			currentTimestamp - mLastProgressTimestamp < request.progressInterval()) {
			return;
		}

		/* save progress timestamp */
		mLastProgressTimestamp = currentTimestamp;

		if (!request.isCanceled()) {
			mDelivery.postProgress(request, bytesWritten, totalBytes);
		}
	}

	/* update download success */
	@SuppressWarnings("ResultOfMethodCallIgnored")
	private void updateSuccess(DownloadRequest request) {
		updateState(request, DownloadState.SUCCESSFUL);
		
		/* notify the request download finish */
		request.finish();

		File file = new File(request.tempFilePath());
		if (file.exists()) {
			file.renameTo(new File(request.getDestFilePath()));
		}

		/* deliver success message */
		mDelivery.postSuccess(request);
	}

	/* update download failure */
	private void updateFailure(DownloadRequest request, int statusCode, String errMsg) {
		updateState(request, DownloadState.FAILURE);

		/* if the status code is 0, may be cause by the net error */
		if ((statusCode == HTTP_INVALID || statusCode == HTTP_ERROR_SIZE) &&
			request.retryTime() >= 0) {
			try {
				/* update progress in case */
				long bytesWritten = new File(request.tempFilePath()).length();
				updateProgress(request, bytesWritten, mTotalBytes);
				
				/* sleep a while before retrying */
				sleep(SLEEP_BEFORE_RETRY);
			} catch (InterruptedException e) {
				/* we may have been interrupted because it was time to quit */
				if (mQuit) {
					request.finish();

					return;
				}
			}
			
			/* retry downloading */
			if (!request.isCanceled()) {
				updateRetry(request);
				executeDownload(request);
			}

			return;
		}
		
		/* notify the request that downloading has finished */
		request.finish();
		
		/* deliver failure message */
		mDelivery.postFailure(request, statusCode, errMsg);
	}

	/* execute downloading */
	private void executeDownload(DownloadRequest request) {
		if (Thread.currentThread().isInterrupted()) {
			return;
		}

		Downloader downloader = request.downloader();
		RandomAccessFile raf = null;
		InputStream is = null;

		try {
			File file = new File(request.tempFilePath());
			raf = new RandomAccessFile(file, "rw");
			long breakpoint = file.length();
			long bytesWritten = 0;
			if (file.exists()) {
				/* set the range to continue the downloading */
				raf.seek(breakpoint);
				bytesWritten = breakpoint;
			}

			int statusCode = downloader.start(request.uri(), breakpoint);
			if (statusCode != HTTP_OK && statusCode != HTTP_PARTIAL) {
				throw new DownloadException(statusCode, "download fail");
			}

			is = downloader.byteStream();
			long contentLength = downloader.contentLength();
			if (contentLength <= 0 && is == null) {
				throw new DownloadException(statusCode, "content length error");
			}
			contentLength += bytesWritten;

			updateStart(request, contentLength);

			if (is != null) {
				byte[] buffer = new byte[BUFFER_SIZE];
				int length;

				while (true) {
					/* if the request has canceld, stop the downloading */
					if (Thread.currentThread().isInterrupted() || request.isCanceled()) {
						request.finish();
						return;
					}

					/* if current is not wifi and mobile network is not allowed, stop */
					if (request.allowedNetworkTypes() != 0 && !Utils.isWifi(request.context()) &&
						(request.allowedNetworkTypes() & DownloadRequest.NETWORK_MOBILE) == 0) {
						throw new DownloadException(statusCode, "network error");
					}

					/* read data into buffer from input stream */
					length = readFromInputStream(buffer, is);
					long fileSize = raf.length();
					long totalBytes = contentLength <= 0 ? fileSize : contentLength;

					if (length == -1) {
						updateSuccess(request);
						return;
					} else if (length == Integer.MIN_VALUE) {
						throw new DownloadException(statusCode, "transfer data error");
					}

					bytesWritten += length;
					/* write buffer into local file */
					raf.write(buffer, 0, length);

					/* deliver progress callback */
					updateProgress(request, bytesWritten, totalBytes);
				}
			} else {
				throw new DownloadException(statusCode, "input stream error");
			}
		} catch (IOException e) {
			if (e instanceof DownloadException) {
				DownloadException exception = (DownloadException) e;
				updateFailure(request, exception.getCode(), exception.getMessage());
			} else {
				updateFailure(request, 0, e.getMessage());
			}
		} finally {
			downloader.close();
			silentCloseFile(raf);
			silentCloseInputStream(is);
		}
	}


	/* read data from input stream */
	private int readFromInputStream(byte[] buffer, InputStream is) {
		try {
			return is.read(buffer);
		} catch (IOException e) {
			if (END_OF_STREAM.equals(e.getMessage())) {
				return -1;
			}

			return Integer.MIN_VALUE;
		}
	}

	/* a utility function to close a random access file without raising an exception */
	static void silentCloseFile(RandomAccessFile raf) {
		if (raf != null) {
			try {
				raf.close();
			} catch (IOException ignore) {
			}
		}
	}

	/* a utility function to close an input stream without raising an exception */
	private static void silentCloseInputStream(InputStream is) {
		try {
			if (is != null) {
				is.close();
			}
		} catch (IOException e) {
			Log.w(TAG, "cannot close input stream", e);
		}
	}

	/**
	 * Forces this dispatcher to quit immediately. If any download requests are still in
	 * the queue, they are not guaranteed to be processed.
	 */
	protected void quit() {
		mQuit = true;
		
		/* interrupt current thread */
		interrupt();

		Log.i(TAG, "download dispatcher has quit");
	}
}
