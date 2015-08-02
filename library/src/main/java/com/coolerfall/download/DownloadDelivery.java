package com.coolerfall.download;

import java.util.concurrent.Executor;

import android.os.Handler;

/**
 * Download delivery: used to delivery callback to call back in main thread.
 * 
 * @author Vincent Cheung
 * @since  Nov. 24, 2014
 */
public class DownloadDelivery {
	private final Executor mDownloadPoster;
	
	public DownloadDelivery(final Handler handler) {
		mDownloadPoster = new Executor() {
			@Override
			public void execute(Runnable command) {
				handler.post(command);
			}
		};
	}
	
	/**
	 * Post download start event.
	 * 
	 * @param request    download request
	 * @param totalBytes total bytes
	 */
	protected void postStart(final DownloadRequest request, final long totalBytes) {
		mDownloadPoster.execute(new Runnable() {
			@Override
			public void run() {
				if (request.getDownloadListener() != null) {
					request.getDownloadListener().onStart(request.getDownloadId(), totalBytes);
				}
			}
		});
	}
	
	/**
	 * Post download retry event.
	 * 
	 * @param request download request
	 */
	protected void postRetry(final DownloadRequest request) {
		mDownloadPoster.execute(new Runnable() {
			@Override
			public void run() {
				if (request.getDownloadListener() != null) {
					request.getDownloadListener().onRetry(request.getDownloadId());
				}
			}
		});
	}
	
	/**
	 * Post download progress event.
	 * 
	 * @param request      download request
	 * @param bytesWritten the bytes have written to file
	 * @param totalBytes   the total bytes of currnet file in downloading
	 */
	protected void postProgress(final DownloadRequest request, 
			final long bytesWritten, final long totalBytes) {
		mDownloadPoster.execute(new Runnable() {
			@Override
			public void run() {
				if (request.getDownloadListener() != null) {
					request.getDownloadListener().onProgress(
							request.getDownloadId(), bytesWritten, totalBytes);
				}
			}
		});
	}
	
	/**
	 * Post download success event.
	 * 
	 * @param request download request
	 */
	protected void postSuccess(final DownloadRequest request) {
		mDownloadPoster.execute(new Runnable() {
			@Override
			public void run() {
				if (request.getDownloadListener() != null) {
					request.getDownloadListener().onSuccess(
							request.getDownloadId(), request.getDestFilePath());
				}

				if (request.getSimpleDownloadListener() != null) {
					request.getSimpleDownloadListener().onSuccess(
							request.getDownloadId(), request.getDestFilePath()
					);
				}
			}
		});
	}

	/**
	 * Post download failure event.
	 *
	 * @param request    download request
	 * @param statusCode status code
	 * @param errMsg     error message
	 */
	protected void postFailure(final DownloadRequest request, final int statusCode, final String errMsg) {
		mDownloadPoster.execute(new Runnable() {
			@Override
			public void run() {
				if (request.getDownloadListener() != null) {
					request.getDownloadListener().onFailure(
							request.getDownloadId(), statusCode, errMsg);
				}

				if (request.getSimpleDownloadListener() != null) {
					request.getSimpleDownloadListener().onFailure(
							request.getDownloadId(), statusCode, errMsg
					);
				}
			}
		});
	}
}
