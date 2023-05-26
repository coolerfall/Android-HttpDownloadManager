## Download Manager

You can costomize `DownloadManager` with the fllowing configurations.

### downloader

* Type: `com.coolerfall.download.Downloader`
* Optional: `true`

You can implement a `Downloader` with any http library. The download manager will detect http
library and choose available `Downloader` to use if not set. The download manager
provides `OkHttpDownloader` and `URLDownloader` currently.

!> If you're using `OkHttpDownloader` with custom `OkHttpClient` as `Downloader`
in `DownloadManager`, then you should not add `HttpLoggingInterceptor` in your custom `OkHttpClient`
. It may be crashed(OOM) as `HttpLoggingInterceptor ` use `okio` to reqeust the whole body in
memory.

### threadPoolSize

* Type: `int`
* Default: `3`

The pool size of the download dispatcher thread. The default size will be set if less than 0 or more
than 10.

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

### logger

* Type: `com.coolerfall.download.Logger`
* Default: `com.coolerfall.download.Logger.EMPTY`

Log necessary information when downloading. If you don't care this, just ignore.

> If you want to custom `DownloadManager`, then use `DownloadManager.with(builder)`.

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

### pack

* Type: `Pack`
* Optional: `false`

`Pack` is the target where the file will be put. Some builtin pack:

* ExtPublicPack: put file in external public directory such public `Download` directory,
  see `Environment.getExternalStoragePublicDirectory`
* ExtFilePack: put file in external files directory, see `Context.getExternalFilesDir`

### priority

* Type: `com.coolerfall.download.Priority`
* Default: `com.coolerfall.download.Priority.Normal`

Higher priority will download first.

### downloadCallback

* Type: `com.coolerfall.download.DownloadCallback`
* Default: `com.coolerfall.download.DownloadCallback.EMPTY`

Provides some callbacks when downloading.

