package com.coolerfall.downloadsample;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.coolerfall.download.DownloadListener;
import com.coolerfall.download.DownloadManager;
import com.coolerfall.download.DownloadRequest;
import com.coolerfall.download.SimpleDownloadListener;

import java.io.File;

public class MainActivity extends Activity implements OnClickListener {
	private static final String TAG = "Vtag";
	private static final String[] URL = {
		"http://gdown.baidu.com/data/wisegame/42f561037ef6d1c5/dianxinbohao_5058.apk",
		"http://gdown.baidu.com/data/wisegame/07140005cb121398/zuimeitianqi_2014112000.apk",
		"http://gdown.baidu.com/data/wisegame/024ebaed2f796a48/wangyiyunyinle_35.apk",
		"http://t.cn/RLGOYCD",
		"http://p.gdown.baidu.com/df1cc8402c66d5a8f724dd5f5824c918ce8360987501ba8c9af58f24a18e6f4e1c0990be72787aa995075b10f4e38427a1b06c1f9db0dce4992cc346c665ff5cd003ff3f09e9b3ba3761ddef6636295e7b852854c7f5263b6a5a7a8fb5326e905950942d3e56d60f6ecd567a1ca04a21f7a4186af1f7e8f82e927cb541f43db73c6ff255e71f631caeea5b247a52feec3fc64636f10f39bf2736a0667bdfb7d22c276629f7be1c1a598fd3450492a4cc0874e191879d1c503c457b0d85676a0009aaabbd0f4b2980c233208afaf611a1eee2bc3785402fbc7753ddea2cc982d59f364259e4b7ee3febfeb5272452773ed714e6be0b23ea0052651f73c5d0e6348b7b21608a317b08e8a46fc094f94d5d55d734d53b211b7282ae5f9c1f97e9ff678d2c19162f6f1ef297b736db87dae72d6bf42f919cf934bd5f0a5a6bb97914ec5854f4c4cbde51",
	};

	private static final int DOWNLOAD_ID0 = 0;
	private static final int DOWNLOAD_ID1 = 1;
	private static final int DOWNLOAD_ID2 = 2;
	private static final int DOWNLOAD_ID3 = 3;
	private static final int DOWNLOAD_ID4 = 4;

	private ProgressBar mProgressBar;
	private ProgressBar mProgressBar1;
	private ProgressBar mProgressBar2;
	private ProgressBar mProgressBar3;
	private ProgressBar mProgressBar4;
	private TextView mTextSpeed;
	private TextView mTextSpeed1;
	private TextView mTextSpeed2;
	private TextView mTextSpeed3;
	private TextView mTextSpeed4;

	private DownloadManager mDownloadManager;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		findViewById(R.id.download_btn_start).setOnClickListener(this);
		findViewById(R.id.download_btn_start1).setOnClickListener(this);
		findViewById(R.id.download_btn_start2).setOnClickListener(this);
		findViewById(R.id.download_btn_start3).setOnClickListener(this);
		findViewById(R.id.download_btn_start4).setOnClickListener(this);

		mProgressBar = (ProgressBar) findViewById(R.id.download_progress);
		mProgressBar1 = (ProgressBar) findViewById(R.id.download_progress1);
		mProgressBar2 = (ProgressBar) findViewById(R.id.download_progress2);
		mProgressBar3 = (ProgressBar) findViewById(R.id.download_progress3);
		mProgressBar4 = (ProgressBar) findViewById(R.id.download_progress4);

		mTextSpeed = (TextView) findViewById(R.id.download_tv_speed0);
		mTextSpeed1 = (TextView) findViewById(R.id.download_tv_speed1);
		mTextSpeed2 = (TextView) findViewById(R.id.download_tv_speed2);
		mTextSpeed3 = (TextView) findViewById(R.id.download_tv_speed3);
		mTextSpeed4 = (TextView) findViewById(R.id.download_tv_speed4);

		mDownloadManager = new DownloadManager();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mDownloadManager.release();
	}
	
	@Override
	public void onClick(View v) {
		int downloadId = 0;
		
		switch (v.getId()) {
		case R.id.download_btn_start:
			downloadId = DOWNLOAD_ID0;
			break;
			
		case R.id.download_btn_start1:
			downloadId = DOWNLOAD_ID1;
			break;
			
		case R.id.download_btn_start2:
			downloadId = DOWNLOAD_ID2;
			break;
			
		case R.id.download_btn_start3:
			downloadId = DOWNLOAD_ID3;
			break;
			
		case R.id.download_btn_start4:
			downloadId = DOWNLOAD_ID4;
			break;

		default:
			break;
		}
		
		if (mDownloadManager.isDownloading(downloadId)) {
			mDownloadManager.cancel(downloadId);
		} else {
			DownloadRequest request = new DownloadRequest()
				.setDownloadId(downloadId)
//				.setDownloadListener(new Listener())
				/* can set simple listener */
				.setSimpleDownloadListener(new SimpleListener())
				.setRetryTime(5)
				.setAllowedNetworkTypes(this, DownloadRequest.NETWORK_WIFI)
				.setUrl(URL[downloadId]);
			mDownloadManager.add(request);
		}
	}
	
	private class Listener implements DownloadListener {
		private long mStartTimestamp = 0;
		private long mLastTimestamp = 0;
		private long mStartSize = 0;
		
		@Override
		public void onStart(int downloadId, long totalBytes) {
			Log.d(TAG, "start download: " + downloadId);
			Log.d(TAG, "totalBytes: " + totalBytes);
			mStartTimestamp = System.currentTimeMillis();
		}
		
		@Override
		public void onRetry(int downloadId) {
			Log.d(TAG, "downloadId: " + downloadId);
		}

		@Override
		public void onProgress(int downloadId, long bytesWritten, long totalBytes) {
			int progress = (int) (bytesWritten * 100f / totalBytes);
			progress = progress == 100 ? 0 : progress;
			long currentTimestamp = System.currentTimeMillis();
			
			int speed;
			if (currentTimestamp - mLastTimestamp >= 300 || progress == 0) {
				mLastTimestamp = currentTimestamp;
				int deltaTime = (int) (currentTimestamp - mStartTimestamp + 1);
				speed = (int) ((bytesWritten - mStartSize) * 1000 / deltaTime) / 1024;
				
				switch (downloadId) {
				case DOWNLOAD_ID0:
					mProgressBar.setProgress(progress);
					mTextSpeed.setText(speed + "kb/s");
					break;
					
				case DOWNLOAD_ID1:
					mProgressBar1.setProgress(progress);
					mTextSpeed1.setText(speed + "kb/s");
					break;
					
				case DOWNLOAD_ID2:
					mProgressBar2.setProgress(progress);
					mTextSpeed2.setText(speed + "kb/s");
					break;
					
				case DOWNLOAD_ID3:
					mProgressBar3.setProgress(progress);
					mTextSpeed3.setText(speed + "kb/s");
					break;
					
				case DOWNLOAD_ID4:
					mProgressBar4.setProgress(progress);
					mTextSpeed4.setText(speed + "kb/s");
					break;

				default:
					break;
				}
			}
		}
		
		@Override
		public void onSuccess(int downloadId, String filePath) {
			Log.d(TAG, "success: " + downloadId + " size: " + new File(filePath).length());
		}
		
		@Override
		public void onFailure(int downloadId, int statusCode, String errMsg) {
			Log.d(TAG, "fail: " + downloadId + " " + statusCode + " " + errMsg);
		}
	}

	private class SimpleListener implements SimpleDownloadListener {
		@Override
		public void onSuccess(int downloadId, String filePath) {
			Log.d(TAG, "simple download listener sucess");
		}

		@Override
		public void onFailure(int downloadId, int statusCode, String errMsg) {
			Log.d(TAG, "simple download listener failt");
		}
	}
}
