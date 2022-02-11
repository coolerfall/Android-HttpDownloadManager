package com.coolerfall.downloadsample;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.coolerfall.download.DownloadCallbackAdapter;
import com.coolerfall.download.DownloadManager;
import com.coolerfall.download.DownloadRequest;
import com.coolerfall.download.Logger;
import com.coolerfall.download.OkHttpDownloader;
import com.coolerfall.download.Priority;
import java.io.File;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;

public class MainActivity extends AppCompatActivity implements OnClickListener {
  private static final String TAG = "HttpDownloadManager";
  private static final String[] URL = {
      "https://f-droid.org/repo/com.gitlab.mahc9kez.shadowsocks.foss_50104000.apk",
      "https://f-droid.org/repo/org.moire.ultrasonic_100.apk",
      "https://f-droid.org/repo/com.simplemobiletools.draw.pro_65.apk",
      "https://f-droid.org/repo/im.vector.app_40103180.apk",
      "https://f-droid.org/repo/it.reyboz.bustorino_38.apk"
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

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    findViewById(R.id.download_btn_start).setOnClickListener(this);
    findViewById(R.id.download_btn_start1).setOnClickListener(this);
    findViewById(R.id.download_btn_start2).setOnClickListener(this);
    findViewById(R.id.download_btn_start3).setOnClickListener(this);
    findViewById(R.id.download_btn_start4).setOnClickListener(this);

    progressBar = findViewById(R.id.download_progress);
    progressBar1 = findViewById(R.id.download_progress1);
    progressBar2 = findViewById(R.id.download_progress2);
    progressBar3 = findViewById(R.id.download_progress3);
    progressBar4 = findViewById(R.id.download_progress4);

    textSpeed = findViewById(R.id.download_tv_speed0);
    textSpeed1 = findViewById(R.id.download_tv_speed1);
    textSpeed2 = findViewById(R.id.download_tv_speed2);
    textSpeed3 = findViewById(R.id.download_tv_speed3);
    textSpeed4 = findViewById(R.id.download_tv_speed4);

    OkHttpClient client = new OkHttpClient.Builder().build();
    downloadManager = new DownloadManager.Builder().context(this)
        .downloader(OkHttpDownloader.create(client))
        .threadPoolSize(3)
        .logger(new Logger() {
          @Override public void log(String message) {
            Log.d(TAG, message);
          }
        })
        .build();
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    downloadManager.release();
  }

  @Override public void onClick(View v) {
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
      DownloadRequest request = new DownloadRequest.Builder().url(URL[index])
          .downloadCallback(new Listener())
          .retryTime(5)
          .retryInterval(5, TimeUnit.SECONDS)
          .progressInterval(1, TimeUnit.SECONDS)
          .priority(index == 4 ? Priority.HIGH : Priority.NORMAL)
          .allowedNetworkTypes(DownloadRequest.NETWORK_ALL)
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

  private class Listener extends DownloadCallbackAdapter {
    private long startTimestamp = 0;
    private long startSize = 0;

    @Override public void onStart(int downloadId, long totalBytes) {
      Log.d(TAG, "start download: " + downloadId);
      Log.d(TAG, "totalBytes: " + totalBytes);
      startTimestamp = System.currentTimeMillis();
    }

    @Override public void onRetry(int downloadId) {
      Log.d(TAG, "retry downloadId: " + downloadId);
    }

    @SuppressLint("SetTextI18n") @Override
    public void onProgress(int downloadId, long bytesWritten, long totalBytes) {
      int progress = (int) (bytesWritten * 100f / totalBytes);
      long currentTimestamp = System.currentTimeMillis();
      Log.d(TAG, "progress: " + progress);

      int speed;
      int deltaTime = (int) (currentTimestamp - startTimestamp + 1);
      speed = (int) ((bytesWritten - startSize) * 1000 / deltaTime) / 1024;
      startSize = bytesWritten;

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

    @Override public void onSuccess(int downloadId, String filepath) {
      Log.d(TAG, "success: " + downloadId + " size: " + new File(filepath).length());
      boolean result = downloadManager.copyToPublicDownloadDir(filepath);
      Log.e(TAG, "result of copying file: " + result);
    }

    @Override public void onFailure(int downloadId, int statusCode, String errMsg) {
      Log.d(TAG, "fail: " + downloadId + " " + statusCode + " " + errMsg);
    }
  }
}
