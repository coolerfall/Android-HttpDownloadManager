## Download Manager

You can costomize `DownloadManager` with the fllowing configurations.


### context

* Type: `android.content.Context`
* Optional: `false`

Application context to get download root directory, this cannot be null.

### downloader

* Type: `com.coolerfall.download.Downloader`
* Optional: `true`

You can implement a `Downloader` with any http library. The download manager will detect http library and choose available `Downloader` to use if not set. The download manager provides `OkHttpDownloader` and `URLDownloader` currently.

!> If you're using `OkHttpDownloader` with custom `OkHttpClient` as `Downloader` in `DownloadManager`, then you should not add `HttpLoggingInterceptor` in your custom `OkHttpClient`. It may be crashed(OOM) as `HttpLoggingInterceptor ` use `okio` to reqeust the whole body in memory.

### threadPoolSize

* Type: `int`
* Default: `3`

The pool size of the download dispatcher thread. The default size will be set if less than 0 or more than 10. 

### logger

* Type: `com.coolerfall.download.Logger`
* Default: `com.coolerfall.download.Logger.EMPTY`

Log necessary information when downloading. If you don't care this, just ignore.

> If you want to copy files to external public download directory, `DownloadManager` provides `copyToPublicDownloadDir(String filepath)`.


## Download Request

A `DownloadRequest` represents a downloading, you can configure it as following.

### url

* Type: `String`

The full url of files to download, such as `https://example.com/readme.md`.

### uri

* Type: `android.net.Uri`

This is an alternative of [url](#url), you can choose one to set.

### downloadId

* Type: `int`
* Optional: `true`

A unique id of `DownloadRequest`, it will be set automatically if not set.

### relativeDirectory

* Type: `String`
* Optional: `true`

After android Q, we can just save files in external private directory and public download directory, so the download manager will use `Context#getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)` as download root directory. The download manager will extract filename from header or url.

### relativeFilepath

* Type: `String`
* Optional: `true`

Set filepath mannully. The download manager will use this filepath instead of auto detecting.

### priority

* Type: `com.coolerfall.download.Priority`
* Default: `com.coolerfall.download.Priority.Normal`

Higher priority will download first.

### retryTime

* Type: `int`
* Default: `3`

How many times you want to retry if download failed for some network problem and so on.

### retryInterval

* Type `long, java.util.concurrent.TimeUnit`
* Default: `30 seconds`

Interval of each retry.

### progressInterval

* Type `long, java.util.concurrent.TimeUnit`
* Default: `100 milliseconds`

Interval of progress refreshing.

### downloadCallback

* Type: `com.coolerfall.download.DownloadCallback`
* Default: `com.coolerfall.download.DownloadCallback.EMPTY`

Provides some callbacks when downloading.

