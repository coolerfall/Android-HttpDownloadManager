package com.coolerfall.download;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static android.os.Environment.DIRECTORY_DOWNLOADS;
import static com.coolerfall.download.Preconditions.checkNotNull;
import static com.coolerfall.download.Utils.copy;
import static com.coolerfall.download.Utils.createDefaultDownloader;
import static com.coolerfall.download.Utils.resolvePath;

/**
 * A manager used to manage the downloading.
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
public final class DownloadManager {
  private final Context context;
  private final Downloader downloader;
  private final int threadPoolSize;
  private final Logger logger;
  private DownloadRequestQueue downloadRequestQueue;
  private final String rootDownloadDir;

  DownloadManager(Builder builder) {
    context = checkNotNull(builder.context, "context == null").getApplicationContext();
    downloader = checkNotNull(builder.downloader, "downloader == null");
    threadPoolSize = builder.threadPoolSize;
    logger = builder.logger;
    downloadRequestQueue = new DownloadRequestQueue(threadPoolSize, logger);
    downloadRequestQueue.start();

    File downlodDir = context.getExternalFilesDir(DIRECTORY_DOWNLOADS);
    checkNotNull(downlodDir, "shared storage is not currently available");
    rootDownloadDir = downlodDir.getAbsolutePath();
  }

  /**
   * Add one download request into the queue.
   *
   * @param request download request
   * @return download id, if the id is not set, then manager will generate one.
   * if the request is in downloading, then -1 will be returned
   */
  public int add(DownloadRequest request) {
    checkNotNull(request, "request == null");
    if (isDownloading(request.uri().toString())) {
      return -1;
    }

    request.context(context);
    request.rootDownloadDir(rootDownloadDir);
    request.downloader(downloader.copy());

    /* add download request into download request queue */
    return downloadRequestQueue.add(request) ? request.downloadId() : -1;
  }

  /**
   * Query download from download request queue.
   *
   * @param downloadId download id
   * @return download state
   */
  public DownloadState query(int downloadId) {
    return downloadRequestQueue.query(downloadId);
  }

  /**
   * Query download from download request queue.
   *
   * @param url download url
   * @return download state
   */
  DownloadState query(String url) {
    return downloadRequestQueue.query(Uri.parse(url));
  }

  /**
   * To check if the download was in the request queue.
   *
   * @param downloadId downalod id
   * @return true if was downloading, otherwise return false
   */
  public boolean isDownloading(int downloadId) {
    return query(downloadId) != DownloadState.INVALID;
  }

  /**
   * To check if the download was in the request queue.
   *
   * @param url downalod url
   * @return true if was downloading, otherwise return false
   */
  public boolean isDownloading(String url) {
    return query(url) != DownloadState.INVALID;
  }

  /**
   * Get the download task size.
   *
   * @return the task size
   */
  public int getTaskSize() {
    return downloadRequestQueue == null ? 0 : downloadRequestQueue.getDownloadingSize();
  }

  /**
   * Cancel the download according to download id.
   *
   * @param downloadId download id
   * @return true if download has canceled, otherwise return false
   */
  public boolean cancel(int downloadId) {
    return downloadRequestQueue.cancel(downloadId);
  }

  /**
   * Cancel all the downloading in queue.
   */
  public void cancelAll() {
    downloadRequestQueue.cancelAll();
  }

  /**
   * Release all the resource.
   */
  public void release() {
    if (downloadRequestQueue != null) {
      downloadRequestQueue.release();
      downloadRequestQueue = null;
    }
  }

  /**
   * Copy downloaded file to external public download directory.
   *
   * @param filepath filepath of downloaded file
   * @return true if copy successfully, otherwise return false
   */
  public boolean copyToPublicDownloadDir(String filepath) {
    File publicDownloadDir =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    String dir = publicDownloadDir.getAbsolutePath();
    if (!filepath.startsWith(rootDownloadDir)) {
      throw new IllegalArgumentException("Only files of current app can be exported");
    }
    String filename = filepath.substring(filepath.lastIndexOf(File.separator) + 1);
    String outputFilepath = resolvePath(dir, filepath.substring(rootDownloadDir.length() + 1));
    FileInputStream fis;
    try (OutputStream os = openOutputStream(outputFilepath, filename)) {
      fis = new FileInputStream(filepath);
      copy(fis, os);
      return true;
    } catch (Exception e) {
      logger.log("Failed to copy file to public download directory: " + e.getMessage());
      return false;
    }
  }

  private OutputStream openOutputStream(String filepath, String filename) throws IOException {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
      ContentValues contentValues = new ContentValues();
      contentValues.put(MediaStore.Files.FileColumns.RELATIVE_PATH,
          Environment.DIRECTORY_DOWNLOADS);
      contentValues.put(MediaStore.Files.FileColumns.DISPLAY_NAME, filename);
      int index = filename.lastIndexOf(".");
      if (index > 0) {
        String mimeType =
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(filename.substring(index + 1));
        contentValues.put(MediaStore.Files.FileColumns.MIME_TYPE, mimeType);
      }
      ContentResolver contentResolver = context.getContentResolver();
      Uri uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
      if (uri == null) {
        throw new IOException("Cannot get shared download directory");
      }
      return contentResolver.openOutputStream(uri);
    } else {
      return new FileOutputStream(filepath);
    }
  }

  public Builder newBuilder() {
    return new Builder(this);
  }

  public static final class Builder {
    private Context context;
    private Downloader downloader;
    private int threadPoolSize;
    private Logger logger;

    public Builder() {
      this.threadPoolSize = 3;
      this.logger = Logger.EMPTY;
    }

    Builder(DownloadManager downloadManager) {
      this.context = downloadManager.context;
      this.downloader = downloadManager.downloader;
      this.threadPoolSize = downloadManager.threadPoolSize;
      this.logger = downloadManager.logger;
    }

    public Builder context(Context context) {
      this.context = context;
      return this;
    }

    public Builder downloader(Downloader downloader) {
      this.downloader = downloader;
      return this;
    }

    public Builder threadPoolSize(int threadPoolSize) {
      this.threadPoolSize = threadPoolSize;
      return this;
    }

    public Builder logger(Logger logger) {
      this.logger = logger;
      return this;
    }

    public DownloadManager build() {
      if (downloader == null) {
        downloader = createDefaultDownloader();
      }
      return new DownloadManager(this);
    }
  }
}
