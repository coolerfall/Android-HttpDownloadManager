

* If you're using `OkHttpDownloader` with custom `OkHttpClient` as `Downloader` in `DownloadManager`, then you should not add [HttpLoggingInterceptor][2] in your custom `OkHttpClient`. It may be crashed(OOM) as `HttpLoggingInterceptor ` use `okio` to reqeust the whole body in memory.

* If you want to copy files to external public download directory, `DownloadManager` provides `copyToPublicDownloadDir(String filepath)`.
