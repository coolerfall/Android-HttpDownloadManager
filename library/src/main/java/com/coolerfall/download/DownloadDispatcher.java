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
	private static final int SLEEP_BEFORE_DOWNLOAD = 1500;
	private static final int SLEEP_BEFORE_RETRY = 3500;
	private static final int BUFFER_SIZE = 4096;
	private static final String END_OF_STREAM = "unexpected end of stream";
	private static final String DEFAULT_THREAD_NAME = "DownloadDispatcher";
	private static final String IDLE_THREAD_NAME = "DownloadDispatcher-Idle";

	private BlockingQueue<DownloadRequest> queue;
	private DownloadDelivery delivery;
	private long lastProgressTimestamp;
	private volatile boolean quit = false;

	/**
	 * Default constructor, with queue and delivery.
	 *
	 * @param queue download request queue
	 * @param delivery download delivery
	 */
	public DownloadDispatcher(BlockingQueue<DownloadRequest> queue, DownloadDelivery delivery) {
		this.queue = queue;
		this.delivery = delivery;
		
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
				request = queue.take();
				sleep(SLEEP_BEFORE_DOWNLOAD);
				setName(DEFAULT_THREAD_NAME);

				/* start download */
				executeDownload(request);
			} catch (InterruptedException e) {
				/* we may have been interrupted because it was time to quit */
				if (quit) {
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
		request.updateDownloadState(state);
	}

	/* update download start state */
	private void updateStart(DownloadRequest request, long totalBytes) {
		/* if the request has failed before, donnot deliver callback */
		if (request.downloadState() == DownloadState.FAILURE) {
			updateState(request, DownloadState.RUNNING);
			return;
		}
		
		/* set the download state of this request as running */
		updateState(request, DownloadState.RUNNING);
		delivery.postStart(request, totalBytes);
	}

	/* update download retrying */
	private void updateRetry(DownloadRequest request) {
		delivery.postRetry(request);
	}

	/* update download progress */
	private void updateProgress(DownloadRequest request, long bytesWritten, long totalBytes) {
		long currentTimestamp = System.currentTimeMillis();
		if (bytesWritten != totalBytes &&
			currentTimestamp - lastProgressTimestamp < request.progressInterval()) {
			return;
		}

		/* save progress timestamp */
		lastProgressTimestamp = currentTimestamp;

		if (!request.isCanceled()) {
			delivery.postProgress(request, bytesWritten, totalBytes);
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
			file.renameTo(new File(request.destinationFilePath()));
		}

		/* deliver success message */
		delivery.postSuccess(request);
	}

	/* update download failure */
	private void updateFailure(DownloadRequest request, int statusCode, String errMsg) {
		updateState(request, DownloadState.FAILURE);

		/* if the status code is 0, may be cause by the net error */
		if (request.retryTime() >= 0) {
			try {
				/* sleep a while before retrying */
				sleep(SLEEP_BEFORE_RETRY);
			} catch (InterruptedException e) {
				/* we may have been interrupted because it was time to quit */
				if (quit) {
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
		delivery.postFailure(request, statusCode, errMsg);
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
	int readFromInputStream(byte[] buffer, InputStream is) {
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
	static void silentCloseInputStream(InputStream is) {
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
	void quit() {
		quit = true;
		
		/* interrupt current thread */
		interrupt();

		Log.i(TAG, "download dispatcher has quit");
	}
}
