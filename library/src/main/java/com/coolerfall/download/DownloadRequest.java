package com.coolerfall.download;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.coolerfall.download.Preconditions.checkNotNull;
import static com.coolerfall.download.Utils.HTTP;
import static com.coolerfall.download.Utils.HTTPS;
import static com.coolerfall.download.Utils.resolvePath;

/**
 * This class represents a request for downloading, this is designed according to Request in
 * Andoird-Volley.
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
public final class DownloadRequest implements Comparable<DownloadRequest> {

  /**
   * Bit flag for all network types.
   */
  public static final int NETWORK_ALL = 0;

  /**
   * Bit flag corresponding to {@link ConnectivityManager#TYPE_MOBILE}.
   */
  public static final int NETWORK_MOBILE = 1;

  /**
   * Bit flag corresponding to {@link ConnectivityManager#TYPE_WIFI}.
   */
  public static final int NETWORK_WIFI = 1 << 1;

  private int downloadId;
  private final AtomicInteger retryTime;
  private int allowedNetworkTypes;
  private Context context;
  private DownloadState downloadState;
  private final Uri uri;
  private final String relativeDirectory;
  private final String relativeFilePath;
  private String destinationFilePath;
  private String destinationDirectory;
  private final long progressInterval;
  private final long retryInterval;
  private DownloadRequestQueue downloadRequestQueue;
  private final long timestamp;
  private final Priority priority;
  private boolean canceled = false;
  private Downloader downloader;
  private final DownloadCallback downloadCallback;

  private DownloadRequest(Builder builder) {
    downloadId = builder.downloadId;
    uri = builder.uri;
    priority = checkNotNull(builder.priority, "priority == null");
    retryTime = new AtomicInteger(builder.retryTime);
    relativeDirectory = builder.relativeDirectory;
    relativeFilePath = builder.relativeFilePath;
    downloadCallback = checkNotNull(builder.downloadCallback, "downloadCallback == null");
    progressInterval = builder.progressInterval;
    retryInterval = builder.retryInterval;
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
    return left == right ? (int) (this.timestamp - other.timestamp)
        : right.ordinal() - left.ordinal();
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
  void downloader(Downloader downloader) {
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
  void downloadRequestQueue(DownloadRequestQueue queue) {
    downloadRequestQueue = queue;

    if (downloadId < 0) {
      downloadId = downloadRequestQueue.getSequenceNumber();
    }
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
   * Get retry interval, used in {@link DownloadDispatcher}.
   *
   * @return retry interval
   */
  long retryInterval() {
    return retryInterval;
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
   * Attach context to this download request to use.
   *
   * @param context {@link Context}
   */
  void context(Context context) {
    this.context = context;
  }

  /**
   * Config root download directory of current app, called by {@link DownloadManager}.
   *
   * @param rootDownloadDir root download dir
   */
  void rootDownloadDir(String rootDownloadDir) {
    destinationDirectory = relativeDirectory == null ? rootDownloadDir
        : resolvePath(rootDownloadDir, relativeDirectory);

    if (relativeFilePath != null) {
      destinationFilePath = resolvePath(rootDownloadDir, destinationFilePath);
      if (new File(destinationFilePath).isDirectory()) {
        throw new IllegalArgumentException("relativeFilePath cannot be a directory");
      }
    }
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
   * Update absolute file path according to the directory and filename.
   *
   * @param filename filename to save
   */
  @SuppressWarnings({ "ResultOfMethodCallIgnored", "ConstantConditions" })
  void updateDestinationFilePath(String filename) {
    /* if the destination path is directory */
    File file = new File(resolvePath(destinationDirectory, filename));
    if (!file.getParentFile().exists()) {
      /* make dirs in case */
      file.getParentFile().mkdirs();
    }

    destinationFilePath = file.toString();
  }

  /**
   * Get destination file path of this download request.
   *
   * @return destination file path
   */
  String destinationFilePath() {
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
   * Get relative directory of current request.
   *
   * @return relative directory
   */
  String relativeDirectory() {
    return relativeDirectory;
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
    private int downloadId = -1;
    private Uri uri;
    private int retryTime;
    private long retryInterval;
    private String relativeDirectory;
    private String relativeFilePath;
    private Priority priority;
    private long progressInterval;
    private int allowedNetworkTypes;
    private DownloadCallback downloadCallback;

    public Builder() {
      this.retryTime = 1;
      this.retryInterval = 3_000;
      this.progressInterval = 100;
      this.priority = Priority.NORMAL;
      this.downloadCallback = DownloadCallbackAdapter.EMPTY_CALLBACK;
    }

    public Builder downloadId(int downloadId) {
      this.downloadId = downloadId;
      return this;
    }

    public Builder url(String url) {
      return uri(Uri.parse(url));
    }

    public Builder uri(Uri uri) {
      this.uri = checkNotNull(uri, "uri == null");
      String scheme = uri.getScheme();
      if (!HTTP.equals(scheme) && !HTTPS.equals(scheme)) {
        throw new IllegalArgumentException("url should start with http or https");
      }
      return this;
    }

    public Builder relativeDirectory(String relativeDirectory) {
      this.relativeDirectory = relativeDirectory;
      return this;
    }

    public Builder relativeFilePath(String relativeFilePath) {
      this.relativeFilePath = relativeFilePath;
      return this;
    }

    public Builder priority(Priority priority) {
      this.priority = priority;
      return this;
    }

    public Builder retryTime(int retryTime) {
      if (retryTime < 0) {
        throw new IllegalArgumentException("retryTime < 0");
      }

      this.retryTime = retryTime;
      return this;
    }

    public Builder retryInterval(long interval, TimeUnit unit) {
      if (interval <= 0) {
        throw new IllegalArgumentException("interval <= 0");
      }

      checkNotNull(unit, "unit == null");
      long millis = unit.toMillis(interval);
      if (millis > Integer.MAX_VALUE) {
        throw new IllegalArgumentException("interval too large");
      }

      this.retryInterval = millis;
      return this;
    }

    public Builder progressInterval(long interval, TimeUnit unit) {
      if (interval < 0) {
        throw new IllegalArgumentException("interval < 0");
      }

      checkNotNull(unit, "unit == null");
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
