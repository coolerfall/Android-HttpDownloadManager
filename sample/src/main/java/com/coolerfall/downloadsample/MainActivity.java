package com.coolerfall.downloadsample;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.coolerfall.download.DownloadCallback;
import com.coolerfall.download.DownloadManager;
import com.coolerfall.download.DownloadRequest;
import com.coolerfall.download.OkHttpDownloader;
import com.coolerfall.download.Priority;

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

	private static final int INDEX_0 = 0;
	private static final int INDEX_1 = 1;
	private static final int INDEX_2 = 2;
	private static final int INDEX_3 = 3;
	private static final int INDEX_4 = 4;
	private SparseIntArray ids = new SparseIntArray();

	private ProgressBar progressBar;
	private ProgressBar progressBar1;
	private ProgressBar progressBar2;
	private ProgressBar progressBar3;
	private ProgressBar progressBar4;
	private TextView textSpeed;
	private TextView textSpeed1;
	private TextView textSpeed2;
	private TextView textSpeed3;
	private TextView textSpeed4;

	private DownloadManager downloadManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		findViewById(R.id.download_btn_start).setOnClickListener(this);
		findViewById(R.id.download_btn_start1).setOnClickListener(this);
		findViewById(R.id.download_btn_start2).setOnClickListener(this);
		findViewById(R.id.download_btn_start3).setOnClickListener(this);
		findViewById(R.id.download_btn_start4).setOnClickListener(this);

		progressBar = (ProgressBar) findViewById(R.id.download_progress);
		progressBar1 = (ProgressBar) findViewById(R.id.download_progress1);
		progressBar2 = (ProgressBar) findViewById(R.id.download_progress2);
		progressBar3 = (ProgressBar) findViewById(R.id.download_progress3);
		progressBar4 = (ProgressBar) findViewById(R.id.download_progress4);

		textSpeed = (TextView) findViewById(R.id.download_tv_speed0);
		textSpeed1 = (TextView) findViewById(R.id.download_tv_speed1);
		textSpeed2 = (TextView) findViewById(R.id.download_tv_speed2);
		textSpeed3 = (TextView) findViewById(R.id.download_tv_speed3);
		textSpeed4 = (TextView) findViewById(R.id.download_tv_speed4);

		downloadManager =
			new DownloadManager.Builder().context(this)
				.downloader(OkHttpDownloader.create())
				.threadPoolSize(2)
				.build();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		downloadManager.release();
	}

	@Override
	public void onClick(View v) {
		int index = 0;

		switch (v.getId()) {
		case R.id.download_btn_start:
			index = INDEX_0;
			break;

		case R.id.download_btn_start1:
			index = INDEX_1;
			break;

		case R.id.download_btn_start2:
			index = INDEX_2;
			break;

		case R.id.download_btn_start3:
			index = INDEX_3;
			break;

		case R.id.download_btn_start4:
			index = INDEX_4;
			break;

		default:
			break;
		}

		int id = ids.get(index, -1);
		if (downloadManager.isDownloading(id)) {
			downloadManager.cancel(id);
		} else {
			DownloadRequest request = new DownloadRequest.Builder()
				.downloadCallback(new Callback())
				.retryTime(5)
				.allowedNetworkTypes(DownloadRequest.NETWORK_WIFI)
				.progressInterval(1000)
				.priority(index == 4 ? Priority.HIGH : Priority.NORMAL)
				.url(URL[index])
				.build();
			int downloadId = downloadManager.add(request);
			ids.put(index, downloadId);
		}
	}

	private int queryIndex(int id) {
		for (int i = 0; i < ids.size(); i++) {
			if (ids.valueAt(i) == id) {
				return ids.keyAt(i);
			}
		}

		return 0;
	}

	private class Callback extends DownloadCallback {
		private long startTimestamp = 0;
		private long startSize = 0;

		@Override
		public void onStart(int downloadId, long totalBytes) {
			Log.d(TAG, "start download: " + downloadId);
			Log.d(TAG, "totalBytes: " + totalBytes);
			startTimestamp = System.currentTimeMillis();
		}

		@Override
		public void onRetry(int downloadId) {
			Log.d(TAG, "retry downloadId: " + downloadId);
		}

		@SuppressLint("SetTextI18n")
		@Override
		public void onProgress(int downloadId, long bytesWritten, long totalBytes) {
			int progress = (int) (bytesWritten * 100f / totalBytes);
			progress = progress == 100 ? 0 : progress;
			long currentTimestamp = System.currentTimeMillis();
			Log.d(TAG, "progress: " + progress);

			int speed;
			int deltaTime = (int) (currentTimestamp - startTimestamp + 1);
			speed = (int) ((bytesWritten - startSize) * 1000 / deltaTime) / 1024;

			int index = queryIndex(downloadId);
			switch (index) {
			case INDEX_0:
				progressBar.setProgress(progress);
				textSpeed.setText(speed + "kb/s");
				break;

			case INDEX_1:
				progressBar1.setProgress(progress);
				textSpeed1.setText(speed + "kb/s");
				break;

			case INDEX_2:
				progressBar2.setProgress(progress);
				textSpeed2.setText(speed + "kb/s");
				break;

			case INDEX_3:
				progressBar3.setProgress(progress);
				textSpeed3.setText(speed + "kb/s");
				break;

			case INDEX_4:
				progressBar4.setProgress(progress);
				textSpeed4.setText(speed + "kb/s");
				break;

			default:
				break;
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
}
